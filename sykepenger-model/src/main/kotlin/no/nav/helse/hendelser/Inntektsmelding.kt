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
    private val førsteFraværsdag: LocalDate?,
    private val beregnetInntekt: Inntekt,
    arbeidsgiverperioder: List<Periode>,
    private val begrunnelseForReduksjonEllerIkkeUtbetalt: String?,
    private val harOpphørAvNaturalytelser: Boolean,
    private val harFlereInntektsmeldinger: Boolean,
    private val avsendersystem: Avsendersystem,
    mottatt: LocalDateTime
) : Hendelse {

    init {
        if (arbeidsgiverperioder.isEmpty() && førsteFraværsdag == null) error("Arbeidsgiverperiode er tom og førsteFraværsdag er null")
    }

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
        data object Altinn: Avsendersystem
        data object LPS: Avsendersystem
        data class Nav(internal val vedtaksperiodeId: UUID, internal val inntektsdato: LocalDate): Avsendersystem
        data class NavSelvbestemt(internal val vedtaksperiodeId: UUID, internal val inntektsdato: LocalDate): Avsendersystem
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

    private var type: Type = KlassiskInntektsmelding
    internal fun valider(vedtaksperioder: List<Vedtaksperiode>, aktivitetslogg: IAktivitetslogg, person: Person): Boolean {
        this.type = when (avsendersystem) {
            Avsendersystem.Altinn,
            Avsendersystem.LPS -> KlassiskInntektsmelding
            is Avsendersystem.Nav -> vedtaksperioder.finn(avsendersystem.vedtaksperiodeId)?.let { Portalinntetksmelding(it, avsendersystem.inntektsdato) } ?: ForkastetPortalinntetksmelding
            is Avsendersystem.NavSelvbestemt -> vedtaksperioder.finn(avsendersystem.vedtaksperiodeId)?.let { Portalinntetksmelding(it, avsendersystem.inntektsdato) } ?: ForkastetPortalinntetksmelding
        }
        return this.type.entering(this, aktivitetslogg, person, vedtaksperioder)
    }

    private sealed interface Type {
        fun entering(inntektsmelding: Inntektsmelding, aktivitetslogg: IAktivitetslogg, person: Person, vedtaksperioder: List<Vedtaksperiode>): Boolean
        fun skalOppdatereVilkårsgrunnlag(inntektsmelding: Inntektsmelding, sykdomstidslinjeperiode: Periode?): Boolean
        fun inntektsdato(inntektsmelding: Inntektsmelding): LocalDate
        fun alternativInntektsdatoForInntekthistorikk(inntektsmelding: Inntektsmelding, alternativInntektsdato: LocalDate): LocalDate?
        fun refusjonsdato(inntektsmelding: Inntektsmelding): LocalDate
        fun førsteFraværsdagForHåndteringAvDager(inntektsmelding: Inntektsmelding): LocalDate?
    }

    private data object KlassiskInntektsmelding: Type {
        override fun entering(inntektsmelding: Inntektsmelding, aktivitetslogg: IAktivitetslogg, person: Person, vedtaksperioder: List<Vedtaksperiode>): Boolean {
            aktivitetslogg.info("Håndterer en klassisk inntektsmelding")
            return true
        }
        override fun skalOppdatereVilkårsgrunnlag(inntektsmelding: Inntektsmelding, sykdomstidslinjeperiode: Periode?): Boolean {
            if (sykdomstidslinjeperiode == null) return false // har ikke noe sykdom for arbeidsgiveren
            return inntektsdato(inntektsmelding) in sykdomstidslinjeperiode
        }
        override fun inntektsdato(inntektsmelding: Inntektsmelding): LocalDate {
            if (inntektsmelding.førsteFraværsdag != null && (inntektsmelding.arbeidsgiverperioder.isEmpty() || inntektsmelding.førsteFraværsdag > inntektsmelding.arbeidsgiverperioder.last().endInclusive.nesteDag)) return inntektsmelding.førsteFraværsdag
            return inntektsmelding.arbeidsgiverperioder.maxOf { it.start }
        }
        override fun alternativInntektsdatoForInntekthistorikk(inntektsmelding: Inntektsmelding, alternativInntektsdato: LocalDate) = alternativInntektsdato.takeUnless { it == inntektsdato(inntektsmelding) }
        override fun refusjonsdato(inntektsmelding: Inntektsmelding): LocalDate {
            return if (inntektsmelding.førsteFraværsdag == null) inntektsmelding.arbeidsgiverperioder.maxOf { it.start }
            else inntektsmelding.arbeidsgiverperioder.map { it.start }.plus(inntektsmelding.førsteFraværsdag).max()
        }
        override fun førsteFraværsdagForHåndteringAvDager(inntektsmelding: Inntektsmelding) = inntektsmelding.førsteFraværsdag
    }

    private data object ForkastetPortalinntetksmelding: Type {
        override fun entering(inntektsmelding: Inntektsmelding, aktivitetslogg: IAktivitetslogg, person: Person, vedtaksperioder: List<Vedtaksperiode>): Boolean {
            aktivitetslogg.funksjonellFeil(Varselkode.RV_IM_26)
            aktivitetslogg.info("Inntektsmelding ikke håndtert")
            val harPeriodeInnenfor16Dager = if (inntektsmelding.arbeidsgiverperioder.isEmpty()) {
                true
            } else {
                vedtaksperioder.påvirkerArbeidsgiverperiode(inntektsmelding.arbeidsgiverperioder.periode()!!)
            }
            person.emitInntektsmeldingIkkeHåndtert(inntektsmelding, inntektsmelding.orgnummer, harPeriodeInnenfor16Dager)
            return false
        }

        override fun skalOppdatereVilkårsgrunnlag(inntektsmelding: Inntektsmelding, sykdomstidslinjeperiode: Periode?) = error("Forventer ikke videre behandling av portalinntektsmelding for forkastet periode")
        override fun inntektsdato(inntektsmelding: Inntektsmelding) = error("Forventer ikke videre behandling av portalinntektsmelding for forkastet periode")
        override fun alternativInntektsdatoForInntekthistorikk(inntektsmelding: Inntektsmelding, alternativInntektsdato: LocalDate) = error("Forventer ikke videre behandling av portalinntektsmelding for forkastet periode")
        override fun refusjonsdato(inntektsmelding: Inntektsmelding) = error("Forventer ikke videre behandling av portalinntektsmelding for forkastet periode")
        override fun førsteFraværsdagForHåndteringAvDager(inntektsmelding: Inntektsmelding) = error("Forventer ikke videre behandling av portalinntektsmelding for forkastet periode")
    }

    private data class Portalinntetksmelding(private val vedtaksperiode: Vedtaksperiode, private val inntektsdato: LocalDate) : Type {
        override fun entering(inntektsmelding: Inntektsmelding, aktivitetslogg: IAktivitetslogg, person: Person, vedtaksperioder: List<Vedtaksperiode>): Boolean {
            aktivitetslogg.info("Håndterer en portalinntektsmelding")
            return true
        }
        override fun skalOppdatereVilkårsgrunnlag(inntektsmelding: Inntektsmelding, sykdomstidslinjeperiode: Periode?) = true
        override fun inntektsdato(inntektsmelding: Inntektsmelding): LocalDate {
            val skjæringstidspunkt = vedtaksperiode.skjæringstidspunkt
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
        override fun refusjonsdato(inntektsmelding: Inntektsmelding) = vedtaksperiode.førsteFraværsdag ?: vedtaksperiode.skjæringstidspunkt
        override fun førsteFraværsdagForHåndteringAvDager(inntektsmelding: Inntektsmelding) = vedtaksperiode.førsteFraværsdag

        private companion object {
            private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
            private val logger = LoggerFactory.getLogger(Portalinntetksmelding::class.java)
        }
    }
}
