package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.etterlevelse.KontekstType
import no.nav.helse.etterlevelse.Subsumsjonskontekst
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.etterlevelse.`§ 8-10 ledd 3`
import no.nav.helse.forrigeDag
import no.nav.helse.hendelser.Avsender.ARBEIDSGIVER
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.hendelser.Periode.Companion.periode
import no.nav.helse.nesteDag
import no.nav.helse.person.Behandlinger
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.ForkastetVedtaksperiode
import no.nav.helse.person.ForkastetVedtaksperiode.Companion.overlapperMed
import no.nav.helse.person.Person
import no.nav.helse.person.Sykmeldingsperioder
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.Vedtaksperiode.Companion.finn
import no.nav.helse.person.Vedtaksperiode.Companion.påvirkerArbeidsgiverperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.inntekt.Inntektsgrunnlag.ArbeidsgiverInntektsopplysningerOverstyringer
import no.nav.helse.person.inntekt.Inntektshistorikk
import no.nav.helse.person.inntekt.Refusjonshistorikk
import no.nav.helse.person.inntekt.Refusjonshistorikk.Refusjon.EndringIRefusjon.Companion.refusjonsopplysninger
import no.nav.helse.person.refusjon.Refusjonsservitør
import no.nav.helse.økonomi.Inntekt
import org.slf4j.LoggerFactory
import no.nav.helse.person.inntekt.Inntektsmelding as InntektsmeldingInntekt

class Inntektsmelding(
    meldingsreferanseId: UUID,
    private val refusjon: Refusjon,
    private val orgnummer: String,
    private val beregnetInntekt: Inntekt,
    arbeidsgiverperioder: List<Periode>,
    private val begrunnelseForReduksjonEllerIkkeUtbetalt: String?,
    private val harOpphørAvNaturalytelser: Boolean,
    private val harFlereInntektsmeldinger: Boolean,
    private val avsendersystem: Avsendersystem,
    mottatt: LocalDateTime
) : Hendelse {

    override val behandlingsporing = Behandlingsporing.Arbeidsgiver(
        organisasjonsnummer = orgnummer
    )
    override val metadata = HendelseMetadata(
        meldingsreferanseId = meldingsreferanseId,
        avsender = ARBEIDSGIVER,
        innsendt = mottatt,
        registrert = mottatt,
        automatiskBehandling = false
    )

    private val arbeidsgiverperioder = arbeidsgiverperioder.grupperSammenhengendePerioder()
    private val dager get() = DagerFraInntektsmelding(
        arbeidsgiverperioder = this.arbeidsgiverperioder,
        førsteFraværsdag = type.førsteFraværsdagForHåndteringAvDager(this),
        mottatt = metadata.registrert,
        begrunnelseForReduksjonEllerIkkeUtbetalt = begrunnelseForReduksjonEllerIkkeUtbetalt,
        avsendersystem = avsendersystem,
        harFlereInntektsmeldinger = harFlereInntektsmeldinger,
        harOpphørAvNaturalytelser = harOpphørAvNaturalytelser,
        hendelse = this
    )
    private var håndtertInntekt = false
    private val dokumentsporing = Dokumentsporing.inntektsmeldingInntekt(meldingsreferanseId)

    internal fun addInntekt(inntektshistorikk: Inntektshistorikk, aktivitetslogg: IAktivitetslogg, alternativInntektsdato: LocalDate) {
        val inntektsdato = type.alternativInntektsdatoForInntekthistorikk(this, alternativInntektsdato) ?: return
        if (!inntektshistorikk.leggTil(InntektsmeldingInntekt(inntektsdato, metadata.meldingsreferanseId, beregnetInntekt))) return
        aktivitetslogg.info("Lagrer inntekt på alternativ inntektsdato $inntektsdato")
    }

    internal fun addInntekt(inntektshistorikk: Inntektshistorikk, subsumsjonslogg: Subsumsjonslogg): LocalDate {
        subsumsjonslogg.logg(`§ 8-10 ledd 3`(beregnetInntekt.årlig, beregnetInntekt.daglig))
        val inntektsdato = type.inntektsdato(this)
        inntektshistorikk.leggTil(InntektsmeldingInntekt(inntektsdato, metadata.meldingsreferanseId, beregnetInntekt))
        return inntektsdato
    }

    private val refusjonsElement get() = Refusjonshistorikk.Refusjon(
        meldingsreferanseId = metadata.meldingsreferanseId,
        førsteFraværsdag = type.refusjonsdato(this),
        arbeidsgiverperioder = arbeidsgiverperioder,
        beløp = refusjon.beløp,
        sisteRefusjonsdag = refusjon.opphørsdato,
        endringerIRefusjon = refusjon.endringerIRefusjon.map { it.tilEndring() },
        tidsstempel = metadata.registrert
    )

    internal val refusjonsservitør get() = Refusjonsservitør.fra(refusjon.refusjonstidslinje(type.refusjonsdato(this), arbeidsgiverperioder, metadata.meldingsreferanseId, metadata.innsendt))

    internal fun leggTilRefusjon(refusjonshistorikk: Refusjonshistorikk) {
        refusjonshistorikk.leggTilRefusjon(refusjonsElement)
    }

    internal fun leggTil(behandlinger: Behandlinger): Boolean {
        håndtertInntekt = true
        return behandlinger.oppdaterDokumentsporing(dokumentsporing)
    }

    internal fun nyeArbeidsgiverInntektsopplysninger(builder: ArbeidsgiverInntektsopplysningerOverstyringer, skjæringstidspunkt: LocalDate) {
        val refusjonshistorikk = Refusjonshistorikk()
        refusjonshistorikk.leggTilRefusjon(refusjonsElement)
        // startskuddet dikterer hvorvidt refusjonsopplysningene skal strekkes tilbake til å fylle gråsonen (perioden mellom skjæringstidspunkt og første refusjonsopplysning)
        // inntektsdato er den dagen refusjonsopplysningen i IM gjelder fom slik at det blir ingen strekking da, bare dersom skjæringstidspunkt brukes
        val startskudd = if (builder.ingenRefusjonsopplysninger(behandlingsporing.organisasjonsnummer)) skjæringstidspunkt else type.refusjonsdato(this)
        val inntektGjelder = skjæringstidspunkt til LocalDate.MAX
        builder.leggTilInntekt(
            ArbeidsgiverInntektsopplysning(
                behandlingsporing.organisasjonsnummer,
                inntektGjelder,
                InntektsmeldingInntekt(type.inntektsdato(this), metadata.meldingsreferanseId, beregnetInntekt),
                refusjonshistorikk.refusjonsopplysninger(startskudd)
            )
        )

    }

    sealed interface Avsendersystem {
        data class Altinn(internal val førsteFraværsdag: LocalDate?): Avsendersystem
        data class LPS(internal val førsteFraværsdag: LocalDate?): Avsendersystem
        data class NavPortal(internal val vedtaksperiodeId: UUID, internal val inntektsdato: LocalDate, internal val forespurt: Boolean): Avsendersystem
    }

    class Refusjon(
        val beløp: Inntekt?,
        val opphørsdato: LocalDate?,
        val endringerIRefusjon: List<EndringIRefusjon> = emptyList()
    ) {

        internal fun refusjonstidslinje(førsteFraværsdag: LocalDate?, arbeidsgiverperioder: List<Periode>, meldingsreferanseId: UUID, tidsstempel: LocalDateTime): Beløpstidslinje {
            val kilde = Kilde(meldingsreferanseId, ARBEIDSGIVER, tidsstempel)
            val startskuddet = startskuddet(førsteFraværsdag, arbeidsgiverperioder)
            val opphørIRefusjon = opphørsdato?.let {
                val sisteRefusjonsdag = maxOf(it, startskuddet.forrigeDag)
                EndringIRefusjon(Inntekt.INGEN, sisteRefusjonsdag.nesteDag)
            }

            val hovedopplysning = EndringIRefusjon(beløp ?: Inntekt.INGEN, startskuddet).takeUnless { it.endringsdato == opphørIRefusjon?.endringsdato }
            val alleRefusjonsopplysninger = listOfNotNull(opphørIRefusjon, hovedopplysning, *endringerIRefusjon.toTypedArray())
                .sortedBy { it.endringsdato }
                .filter { it.endringsdato >= startskuddet }
                .filter { it.endringsdato <= (opphørIRefusjon?.endringsdato ?: LocalDate.MAX) }

            check(alleRefusjonsopplysninger.isNotEmpty()) {"Inntektsmeldingen inneholder ingen refusjonsopplysninger. Hvordan er dette mulig?"}

            val sisteBit = Beløpstidslinje.fra(alleRefusjonsopplysninger.last().endringsdato.somPeriode(), alleRefusjonsopplysninger.last().beløp, kilde)
            val refusjonstidslinje = alleRefusjonsopplysninger
                .zipWithNext { nåværende, neste ->
                    Beløpstidslinje.fra(nåværende.endringsdato.somPeriode().oppdaterTom(neste.endringsdato.forrigeDag), nåværende.beløp, kilde)
                }
                .fold(sisteBit) { acc, beløpstidslinje -> acc + beløpstidslinje }

            return refusjonstidslinje
        }

        private fun startskuddet(førsteFraværsdag: LocalDate?, arbeidsgiverperioder: List<Periode>) =
            if (førsteFraværsdag == null) arbeidsgiverperioder.maxOf { it.start }
            else arbeidsgiverperioder.map { it.start }.plus(førsteFraværsdag).max()

        class EndringIRefusjon(
            internal val beløp: Inntekt,
            internal val endringsdato: LocalDate
        ) {

            internal fun tilEndring() = Refusjonshistorikk.Refusjon.EndringIRefusjon(beløp, endringsdato)

            internal companion object {
                internal fun List<EndringIRefusjon>.minOf(opphørsdato: LocalDate?) =
                    (map { it.endringsdato } + opphørsdato).filterNotNull().minOrNull()
            }
        }
    }

    internal fun dager(): DagerFraInntektsmelding {
        return dager
    }

    internal fun ikkeHåndert(
        aktivitetslogg: IAktivitetslogg,
        person: Person,
        vedtaksperioder: List<Vedtaksperiode>,
        forkastede: List<ForkastetVedtaksperiode>,
        sykmeldingsperioder: Sykmeldingsperioder,
        dager: DagerFraInntektsmelding
    ) {
        if (håndtertNå()) return
        aktivitetslogg.info("Inntektsmelding ikke håndtert")
        val relevanteSykmeldingsperioder = sykmeldingsperioder.overlappendePerioder(dager) + sykmeldingsperioder.perioderInnenfor16Dager(dager)
        val overlapperMedForkastet = forkastede.overlapperMed(dager)
        if (relevanteSykmeldingsperioder.isNotEmpty() && !overlapperMedForkastet) {
            person.emitInntektsmeldingFørSøknadEvent(metadata.meldingsreferanseId, relevanteSykmeldingsperioder, behandlingsporing.organisasjonsnummer)
            return aktivitetslogg.info("Inntektsmelding er relevant for sykmeldingsperioder $relevanteSykmeldingsperioder")
        }
        person.emitInntektsmeldingIkkeHåndtert(this, behandlingsporing.organisasjonsnummer, dager.harPeriodeInnenfor16Dager(vedtaksperioder))
    }
    private fun håndtertNå() = håndtertInntekt
    internal fun subsumsjonskontekst() = Subsumsjonskontekst(
        type = KontekstType.Inntektsmelding,
        verdi = metadata.meldingsreferanseId.toString()
    )

    internal fun skalOppdatereVilkårsgrunnlag(sykdomstidslinjeperiode: Periode?) = type.skalOppdatereVilkårsgrunnlag(this, sykdomstidslinjeperiode)

    private lateinit var type: Type

    internal interface Valideringsgrunnlag {
        fun vedtaksperiode(vedtaksperiodeId: UUID): MinimalVedtaksperiode?
        fun inntektsmeldingIkkeHåndtert(inntektsmelding: Inntektsmelding)

        data class MinimalVedtaksperiode(val førsteFraværsdag: () -> LocalDate?, val skjæringstidspunkt: () -> LocalDate)
        data class DefaultValideringsgrunnlag(private val vedtaksperioder: List<Vedtaksperiode>, private val person: Person): Valideringsgrunnlag {
            override fun vedtaksperiode(vedtaksperiodeId: UUID): MinimalVedtaksperiode? {
                val vedtaksperiode = vedtaksperioder.finn(vedtaksperiodeId) ?: return null
                return MinimalVedtaksperiode(vedtaksperiode::førsteFraværsdag, vedtaksperiode::skjæringstidspunkt)
            }

            override fun inntektsmeldingIkkeHåndtert(inntektsmelding: Inntektsmelding) {
                if (inntektsmelding.arbeidsgiverperioder.isEmpty()) {
                    return person.emitInntektsmeldingIkkeHåndtert(inntektsmelding, inntektsmelding.orgnummer, true)
                }
                person.emitInntektsmeldingIkkeHåndtert(
                    hendelse = inntektsmelding,
                    organisasjonsnummer = inntektsmelding.orgnummer,
                    harPeriodeInnenfor16Dager = vedtaksperioder.påvirkerArbeidsgiverperiode(inntektsmelding.arbeidsgiverperioder.periode()!!)
                )
            }
        }
    }

    internal fun valider(valideringsgrunnlag: Valideringsgrunnlag, aktivitetslogg: IAktivitetslogg): Boolean {
        this.type = when (avsendersystem) {
            is Avsendersystem.Altinn -> KlassiskInntektsmelding(avsendersystem.førsteFraværsdag)
            is Avsendersystem.LPS -> KlassiskInntektsmelding(avsendersystem.førsteFraværsdag)
            is Avsendersystem.NavPortal -> valideringsgrunnlag.vedtaksperiode(avsendersystem.vedtaksperiodeId)?.let { Portalinntetksmelding(it, avsendersystem.inntektsdato) } ?: ForkastetPortalinntektsmelding
        }
        aktivitetslogg.info("Håndterer inntektsmelding som ${type::class.simpleName}. Avsendersystem $avsendersystem")
        return this.type.entering(this, aktivitetslogg) { valideringsgrunnlag.inntektsmeldingIkkeHåndtert(this) }
    }

    internal fun valider(vedtaksperioder: List<Vedtaksperiode>, aktivitetslogg: IAktivitetslogg, person: Person) = valider(Valideringsgrunnlag.DefaultValideringsgrunnlag(vedtaksperioder, person), aktivitetslogg)

    private sealed interface Type {
        fun entering(inntektsmelding: Inntektsmelding, aktivitetslogg: IAktivitetslogg, inntektsmeldingIkkeHåndtert: () -> Unit): Boolean
        fun skalOppdatereVilkårsgrunnlag(inntektsmelding: Inntektsmelding, sykdomstidslinjeperiode: Periode?): Boolean
        fun inntektsdato(inntektsmelding: Inntektsmelding): LocalDate
        fun alternativInntektsdatoForInntekthistorikk(inntektsmelding: Inntektsmelding, alternativInntektsdato: LocalDate): LocalDate?
        fun refusjonsdato(inntektsmelding: Inntektsmelding): LocalDate
        fun førsteFraværsdagForHåndteringAvDager(inntektsmelding: Inntektsmelding): LocalDate?
    }

    private data class KlassiskInntektsmelding(private val førsteFraværsdag: LocalDate?): Type {
        override fun entering(inntektsmelding: Inntektsmelding, aktivitetslogg: IAktivitetslogg, inntektsmeldingIkkeHåndtert: () -> Unit): Boolean {
            if (inntektsmelding.arbeidsgiverperioder.isEmpty() && førsteFraværsdag == null) error("Arbeidsgiverperiode er tom og førsteFraværsdag er null")
            return true
        }
        override fun skalOppdatereVilkårsgrunnlag(inntektsmelding: Inntektsmelding, sykdomstidslinjeperiode: Periode?): Boolean {
            if (sykdomstidslinjeperiode == null) return false // har ikke noe sykdom for arbeidsgiveren
            return inntektsdato(inntektsmelding) in sykdomstidslinjeperiode
        }
        override fun inntektsdato(inntektsmelding: Inntektsmelding): LocalDate {
            if (førsteFraværsdag != null && (inntektsmelding.arbeidsgiverperioder.isEmpty() || førsteFraværsdag > inntektsmelding.arbeidsgiverperioder.last().endInclusive.nesteDag)) return førsteFraværsdag
            return inntektsmelding.arbeidsgiverperioder.maxOf { it.start }
        }
        override fun alternativInntektsdatoForInntekthistorikk(inntektsmelding: Inntektsmelding, alternativInntektsdato: LocalDate) = alternativInntektsdato.takeUnless { it == inntektsdato(inntektsmelding) }
        override fun refusjonsdato(inntektsmelding: Inntektsmelding): LocalDate {
            return if (førsteFraværsdag == null) inntektsmelding.arbeidsgiverperioder.maxOf { it.start }
            else inntektsmelding.arbeidsgiverperioder.map { it.start }.plus(førsteFraværsdag).max()
        }
        override fun førsteFraværsdagForHåndteringAvDager(inntektsmelding: Inntektsmelding) = førsteFraværsdag
    }

    private data object ForkastetPortalinntektsmelding: Type {
        override fun entering(inntektsmelding: Inntektsmelding, aktivitetslogg: IAktivitetslogg, inntektsmeldingIkkeHåndtert: () -> Unit): Boolean {
            aktivitetslogg.funksjonellFeil(Varselkode.RV_IM_26)
            aktivitetslogg.info("Inntektsmelding ikke håndtert")
            inntektsmeldingIkkeHåndtert()
            return false
        }
        override fun skalOppdatereVilkårsgrunnlag(inntektsmelding: Inntektsmelding, sykdomstidslinjeperiode: Periode?) = error("Forventer ikke videre behandling av portalinntektsmelding for forkastet periode")
        override fun inntektsdato(inntektsmelding: Inntektsmelding) = error("Forventer ikke videre behandling av portalinntektsmelding for forkastet periode")
        override fun alternativInntektsdatoForInntekthistorikk(inntektsmelding: Inntektsmelding, alternativInntektsdato: LocalDate) = error("Forventer ikke videre behandling av portalinntektsmelding for forkastet periode")
        override fun refusjonsdato(inntektsmelding: Inntektsmelding) = error("Forventer ikke videre behandling av portalinntektsmelding for forkastet periode")
        override fun førsteFraværsdagForHåndteringAvDager(inntektsmelding: Inntektsmelding) = error("Forventer ikke videre behandling av portalinntektsmelding for forkastet periode")
    }

    private data class Portalinntetksmelding(private val minimalVedtaksperiode: Valideringsgrunnlag.MinimalVedtaksperiode, private val inntektsdato: LocalDate) : Type {
        override fun entering(inntektsmelding: Inntektsmelding, aktivitetslogg: IAktivitetslogg, inntektsmeldingIkkeHåndtert: () -> Unit) = true
        override fun skalOppdatereVilkårsgrunnlag(inntektsmelding: Inntektsmelding, sykdomstidslinjeperiode: Periode?) = true
        override fun inntektsdato(inntektsmelding: Inntektsmelding): LocalDate {
            val skjæringstidspunkt = minimalVedtaksperiode.skjæringstidspunkt()
            if (skjæringstidspunkt != inntektsdato) {
                "Inntekt lagres på en annen dato enn oppgitt i portalinntektsmelding for inntektsmeldingId ${inntektsmelding.metadata.meldingsreferanseId}. Inntektsmelding oppga inntektsdato $inntektsdato, men inntekten ble lagret på skjæringstidspunkt $skjæringstidspunkt"
                    .let {
                        logger.info(it)
                        sikkerlogg.info(it)
                    }
            }
            return skjæringstidspunkt
        }
        override fun alternativInntektsdatoForInntekthistorikk(inntektsmelding: Inntektsmelding, alternativInntektsdato: LocalDate) = null
        override fun refusjonsdato(inntektsmelding: Inntektsmelding) = minimalVedtaksperiode.førsteFraværsdag() ?: minimalVedtaksperiode.skjæringstidspunkt()
        override fun førsteFraværsdagForHåndteringAvDager(inntektsmelding: Inntektsmelding) = minimalVedtaksperiode.førsteFraværsdag()

        private companion object {
            private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
            private val logger = LoggerFactory.getLogger(Portalinntetksmelding::class.java)
        }
    }
}
