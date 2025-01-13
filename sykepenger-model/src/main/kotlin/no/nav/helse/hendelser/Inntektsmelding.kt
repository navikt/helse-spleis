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
import no.nav.helse.mapWithNext
import no.nav.helse.nesteDag
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.ForkastetVedtaksperiode
import no.nav.helse.person.ForkastetVedtaksperiode.Companion.overlapperMed
import no.nav.helse.person.Person
import no.nav.helse.person.Sykmeldingsperioder
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.person.inntekt.Inntektshistorikk
import no.nav.helse.person.inntekt.Inntektsmeldinginntekt
import no.nav.helse.person.inntekt.Refusjonshistorikk
import no.nav.helse.person.refusjon.Refusjonsservitør
import no.nav.helse.økonomi.Inntekt

class Inntektsmelding(
    meldingsreferanseId: UUID,
    private val refusjon: Refusjon,
    orgnummer: String,
    private val beregnetInntekt: Inntekt,
    arbeidsgiverperioder: List<Periode>,
    private val begrunnelseForReduksjonEllerIkkeUtbetalt: String?,
    private val opphørAvNaturalytelser: List<OpphørAvNaturalytelse>,
    private val harFlereInntektsmeldinger: Boolean,
    private val avsendersystem: Avsendersystem,
    mottatt: LocalDateTime
) : Hendelse {
    private val førsteFraværsdag = when (avsendersystem) {
        is Avsendersystem.LPS -> avsendersystem.førsteFraværsdag
        is Avsendersystem.Altinn -> avsendersystem.førsteFraværsdag
    }

    init {
        // TODO: Første fraværsdag kan gå tilbake til root, og vi trenger ikke noe avsendersystem..?
        
        if (arbeidsgiverperioder.isEmpty() && førsteFraværsdag == null) {
            error("Inntektsmelding må enten ha første fraværsdag eller arbeidsgiverperioder satt.")
        }
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

    private val grupperteArbeidsgiverperioder = arbeidsgiverperioder.grupperSammenhengendePerioder()
    private val dager by lazy {
        DagerFraInntektsmelding(
            arbeidsgiverperioder = grupperteArbeidsgiverperioder,
            førsteFraværsdag = førsteFraværsdag,
            mottatt = metadata.registrert,
            begrunnelseForReduksjonEllerIkkeUtbetalt = begrunnelseForReduksjonEllerIkkeUtbetalt,
            harFlereInntektsmeldinger = harFlereInntektsmeldinger,
            opphørAvNaturalytelser = opphørAvNaturalytelser,
            hendelse = this
        )
    }

    private var håndtertInntekt = false
    val dokumentsporing = Dokumentsporing.inntektsmeldingInntekt(meldingsreferanseId)

    fun korrigertInntekt() = Inntektsmeldinginntekt(inntektsdato, metadata.meldingsreferanseId, beregnetInntekt)

    internal fun addInntekt(inntektshistorikk: Inntektshistorikk, aktivitetslogg: IAktivitetslogg, alternativInntektsdato: LocalDate) {
        val inntektsdato = alternativInntektsdato.takeUnless { it == inntektsdato } ?: return
        if (!inntektshistorikk.leggTil(Inntektsmeldinginntekt(inntektsdato, metadata.meldingsreferanseId, beregnetInntekt))) return
        aktivitetslogg.info("Lagrer inntekt på alternativ inntektsdato $inntektsdato")
    }

    internal fun addInntekt(inntektshistorikk: Inntektshistorikk, subsumsjonslogg: Subsumsjonslogg): LocalDate {
        subsumsjonslogg.logg(`§ 8-10 ledd 3`(beregnetInntekt.årlig, beregnetInntekt.daglig))
        inntektshistorikk.leggTil(Inntektsmeldinginntekt(inntektsdato, metadata.meldingsreferanseId, beregnetInntekt))
        return inntektsdato
    }

    internal val inntektsdato: LocalDate by lazy {
        if (førsteFraværsdag != null && (grupperteArbeidsgiverperioder.isEmpty() || førsteFraværsdag > grupperteArbeidsgiverperioder.last().endInclusive.nesteDag)) førsteFraværsdag
        else grupperteArbeidsgiverperioder.maxOf { it.start }
    }

    private val refusjonsdato: LocalDate by lazy {
        if (førsteFraværsdag == null) grupperteArbeidsgiverperioder.maxOf { it.start }
        else grupperteArbeidsgiverperioder.map { it.start }.plus(førsteFraværsdag).max()
    }

    private val refusjonsElement
        get() = Refusjonshistorikk.Refusjon(
            meldingsreferanseId = metadata.meldingsreferanseId,
            førsteFraværsdag = refusjonsdato,
            arbeidsgiverperioder = grupperteArbeidsgiverperioder,
            beløp = refusjon.beløp,
            sisteRefusjonsdag = refusjon.opphørsdato,
            endringerIRefusjon = refusjon.endringerIRefusjon.map { it.tilEndring() },
            tidsstempel = metadata.registrert
        )

    internal val refusjonsservitør get() = Refusjonsservitør.fra(refusjon.refusjonstidslinje(refusjonsdato, metadata.meldingsreferanseId, metadata.innsendt))

    internal fun leggTilRefusjon(refusjonshistorikk: Refusjonshistorikk) {
        refusjonshistorikk.leggTilRefusjon(refusjonsElement)
    }

    internal fun inntektHåndtert() {
        håndtertInntekt = true
    }

    data class OpphørAvNaturalytelse(
        val beløp: Inntekt,
        val fom: LocalDate,
        val naturalytelse: String
    )

    sealed interface Avsendersystem {
        data class Altinn(internal val førsteFraværsdag: LocalDate?) : Avsendersystem
        data class LPS(internal val førsteFraværsdag: LocalDate?) : Avsendersystem
    }

    class Refusjon(
        val beløp: Inntekt?,
        val opphørsdato: LocalDate?,
        val endringerIRefusjon: List<EndringIRefusjon> = emptyList()
    ) {

        internal fun refusjonstidslinje(refusjonsdato: LocalDate, meldingsreferanseId: UUID, tidsstempel: LocalDateTime): Beløpstidslinje {
            val kilde = Kilde(meldingsreferanseId, ARBEIDSGIVER, tidsstempel)

            val opphørIRefusjon = opphørsdato?.let {
                val sisteRefusjonsdag = maxOf(it, refusjonsdato.forrigeDag)
                EndringIRefusjon(Inntekt.INGEN, sisteRefusjonsdag.nesteDag)
            }

            val hovedopplysning = EndringIRefusjon(beløp ?: Inntekt.INGEN, refusjonsdato).takeUnless { it.endringsdato == opphørIRefusjon?.endringsdato }

            val gyldigeEndringer = endringerIRefusjon
                .filter { it.endringsdato > refusjonsdato }
                .filter { it.endringsdato < (opphørIRefusjon?.endringsdato ?: LocalDate.MAX) }
                .distinctBy { it.endringsdato }

            val alleRefusjonsopplysninger = listOfNotNull(hovedopplysning, *gyldigeEndringer.toTypedArray(), opphørIRefusjon).sortedBy { it.endringsdato }

            check(alleRefusjonsopplysninger.isNotEmpty()) { "Inntektsmeldingen inneholder ingen refusjonsopplysninger. Hvordan er dette mulig?" }

            return alleRefusjonsopplysninger.mapWithNext { nåværende, neste ->
                val fom = nåværende.endringsdato
                val tom = neste?.endringsdato?.forrigeDag ?: fom
                Beløpstidslinje.fra(fom til tom, nåværende.beløp, kilde)
            }.reduce(Beløpstidslinje::plus)
        }

        class EndringIRefusjon(
            internal val beløp: Inntekt,
            internal val endringsdato: LocalDate
        ) {
            internal fun tilEndring() = Refusjonshistorikk.Refusjon.EndringIRefusjon(beløp, endringsdato)
        }
    }

    internal fun dager(): DagerFraInntektsmelding {
        return dager
    }

    internal fun ferdigstill(
        aktivitetslogg: IAktivitetslogg,
        person: Person,
        vedtaksperioder: List<Vedtaksperiode>,
        forkastede: List<ForkastetVedtaksperiode>,
        sykmeldingsperioder: Sykmeldingsperioder
    ) {
        if (håndtertInntekt) return // Definisjonen av om en inntektsmelding er håndtert eller ikke er at vi har håndtert inntekten i den... 🤡
        val relevanteSykmeldingsperioder = sykmeldingsperioder.overlappendePerioder(dager) + sykmeldingsperioder.perioderInnenfor16Dager(dager)
        val overlapperMedForkastet = forkastede.overlapperMed(dager)
        val harPeriodeInnenfor16Dager = dager.harPeriodeInnenfor16Dager(vedtaksperioder)
        if (relevanteSykmeldingsperioder.isNotEmpty() && !overlapperMedForkastet) {
            person.emitInntektsmeldingFørSøknadEvent(metadata.meldingsreferanseId, relevanteSykmeldingsperioder, behandlingsporing.organisasjonsnummer)
            return aktivitetslogg.info("Inntektsmelding før søknad - er relevant for sykmeldingsperioder $relevanteSykmeldingsperioder")
        }
        aktivitetslogg.info("Inntektsmelding ikke håndtert")
        person.emitInntektsmeldingIkkeHåndtert(this, behandlingsporing.organisasjonsnummer, harPeriodeInnenfor16Dager)
    }

    internal fun subsumsjonskontekst() = Subsumsjonskontekst(
        type = KontekstType.Inntektsmelding,
        verdi = metadata.meldingsreferanseId.toString()
    )

    internal fun skalOppdatereVilkårsgrunnlag(sykdomstidslinjeperiode: Periode?): Boolean {
        if (sykdomstidslinjeperiode == null) return false // har ikke noe sykdom for arbeidsgiveren
        return inntektsdato in sykdomstidslinjeperiode
    }
}
