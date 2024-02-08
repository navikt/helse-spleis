package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.etterlevelse.MaskinellJurist
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.hendelser.Avsender.ARBEIDSGIVER
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.hendelser.Periode.Companion.periode
import no.nav.helse.hendelser.inntektsmelding.DagerFraInntektsmelding
import no.nav.helse.nesteDag
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.Generasjoner
import no.nav.helse.person.Person
import no.nav.helse.person.Sykmeldingsperioder
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.inntekt.Inntektshistorikk
import no.nav.helse.person.inntekt.Inntektsmelding
import no.nav.helse.person.inntekt.Refusjonshistorikk
import no.nav.helse.person.inntekt.Refusjonshistorikk.Refusjon.EndringIRefusjon.Companion.refusjonsopplysninger
import no.nav.helse.person.inntekt.Sykepengegrunnlag.ArbeidsgiverInntektsopplysningerOverstyringer
import no.nav.helse.økonomi.Inntekt

class Inntektsmelding(
    meldingsreferanseId: UUID,
    private val refusjon: Refusjon,
    orgnummer: String,
    fødselsnummer: String,
    aktørId: String,
    private val førsteFraværsdag: LocalDate?,
    private val inntektsdato: LocalDate?,
    private val beregnetInntekt: Inntekt,
    arbeidsgiverperioder: List<Periode>,
    private val arbeidsforholdId: String?,
    private val begrunnelseForReduksjonEllerIkkeUtbetalt: String?,
    harOpphørAvNaturalytelser: Boolean = false,
    harFlereInntektsmeldinger: Boolean,
    private val avsendersystem: Avsendersystem?,
    private val mottatt: LocalDateTime,
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : ArbeidstakerHendelse(
    meldingsreferanseId = meldingsreferanseId,
    fødselsnummer = fødselsnummer,
    aktørId = aktørId,
    organisasjonsnummer = orgnummer,
    aktivitetslogg = aktivitetslogg
) {
    companion object {
        fun aktuellForReplay(sammenhengendePeriode: Periode, førsteFraværsdag: LocalDate?, arbeidsgiverperiode: Periode?, redusertUtbetaling: Boolean) : Boolean {
            if (arbeidsgiverperiode == null) return redusertUtbetaling && førsteFraværsdag in sammenhengendePeriode // dersom IM har oppgitt reduksjon, og AGP er tom, da benyttes første fraværsdag som en nødløsning (TM)
            if (arbeidsgiverperiode.overlapperMed(sammenhengendePeriode)) return true
            if (arbeidsgiverperiode.erRettFør(sammenhengendePeriode)) return true // arbeidsgiverperiode f.eks. slutter på fredag & søknaden starter på mandag
            if (sammenhengendePeriode.erRettFør(arbeidsgiverperiode)) return true // om f.eks. søknad slutter på fredag og arbedisgiverperiode starter på mandag
            if (førsteFraværsdag in sammenhengendePeriode) return true
            return false
        }

        private fun inntektdato(førsteFraværsdag: LocalDate?, arbeidsgiverperioder: List<Periode>, inntektsdato: LocalDate?): LocalDate {
            if (inntektsdato != null) return inntektsdato
            if (førsteFraværsdag != null && (arbeidsgiverperioder.isEmpty() || førsteFraværsdag > arbeidsgiverperioder.last().endInclusive.nesteDag)) return førsteFraværsdag
            return arbeidsgiverperioder.maxOf { it.start }
        }
    }

    init {
        val ingenInntektsdatoUtenomPortal = inntektsdato == null && avsendersystem != Avsendersystem.NAV_NO
        val inntektsdatoKunHvisPortal = inntektsdato != null && avsendersystem == Avsendersystem.NAV_NO
        check(ingenInntektsdatoUtenomPortal || inntektsdatoKunHvisPortal) {
            "Om avsendersystem er NAV_NO må inntektsdato være satt og motsatt! inntektsdato=$inntektsdato, avsendersystem=$avsendersystem"
        }
        if (arbeidsgiverperioder.isEmpty() && førsteFraværsdag == null) logiskFeil("Arbeidsgiverperiode er tom og førsteFraværsdag er null")
    }

    private val arbeidsgiverperioder = arbeidsgiverperioder.grupperSammenhengendePerioder()
    private val arbeidsgiverperiode = this.arbeidsgiverperioder.periode()
    private val dager = DagerFraInntektsmelding(
        this.arbeidsgiverperioder,
        førsteFraværsdag,
        mottatt,
        begrunnelseForReduksjonEllerIkkeUtbetalt,
        avsendersystem,
        harFlereInntektsmeldinger,
        harOpphørAvNaturalytelser,
        this
    )
    private var håndtertInntekt = false
    private val beregnetInntektsdato = inntektdato(førsteFraværsdag, this.arbeidsgiverperioder, this.inntektsdato)

    internal fun aktuellForReplay(sammenhengendePeriode: Periode) = Companion.aktuellForReplay(sammenhengendePeriode, førsteFraværsdag, arbeidsgiverperiode, !begrunnelseForReduksjonEllerIkkeUtbetalt.isNullOrBlank())

    internal fun addInntekt(inntektshistorikk: Inntektshistorikk, alternativInntektsdato: LocalDate) {
        if (alternativInntektsdato == this.beregnetInntektsdato) return
        if (!inntektshistorikk.leggTil(Inntektsmelding(alternativInntektsdato, meldingsreferanseId(), beregnetInntekt))) return
        info("Lagrer inntekt på alternativ inntektsdato $alternativInntektsdato")
    }

    internal fun addInntekt(inntektshistorikk: Inntektshistorikk, subsumsjonObserver: SubsumsjonObserver): LocalDate {
        val (årligInntekt, dagligInntekt) = beregnetInntekt.reflection { årlig, _, daglig, _ -> årlig to daglig }
        subsumsjonObserver.`§ 8-10 ledd 3`(årligInntekt, dagligInntekt)
        inntektshistorikk.leggTil(Inntektsmelding(beregnetInntektsdato, meldingsreferanseId(), beregnetInntekt))
        return beregnetInntektsdato
    }

    internal fun leggTilRefusjon(refusjonshistorikk: Refusjonshistorikk) {
        refusjon.leggTilRefusjon(refusjonshistorikk, meldingsreferanseId(), førsteFraværsdag, arbeidsgiverperioder)
    }

    internal fun leggTil(generasjoner: Generasjoner): Boolean {
        håndtertInntekt = true
        return generasjoner.oppdaterDokumentsporing(Dokumentsporing.inntektsmeldingInntekt(meldingsreferanseId()))
    }


    internal fun nyeArbeidsgiverInntektsopplysninger(builder: ArbeidsgiverInntektsopplysningerOverstyringer, skjæringstidspunkt: LocalDate) {
        val refusjonshistorikk = Refusjonshistorikk()
        refusjon.leggTilRefusjon(refusjonshistorikk, meldingsreferanseId(), førsteFraværsdag, arbeidsgiverperioder)
        // startskuddet dikterer hvorvidt refusjonsopplysningene skal strekkes tilbake til å fylle gråsonen (perioden mellom skjæringstidspunkt og første refusjonsopplysning)
        // inntektsdato er den dagen refusjonsopplysningen i IM gjelder fom slik at det blir ingen strekking da, bare dersom skjæringstidspunkt brukes
        val startskudd = if (builder.ingenRefusjonsopplysninger(organisasjonsnummer)) skjæringstidspunkt else beregnetInntektsdato
        builder.leggTilInntekt(
            ArbeidsgiverInntektsopplysning(
                organisasjonsnummer,
                skjæringstidspunkt til LocalDate.MAX,
                Inntektsmelding(beregnetInntektsdato, meldingsreferanseId(), beregnetInntekt),
                refusjonshistorikk.refusjonsopplysninger(startskudd, this)
            )
        )

    }

    override fun innsendt() = mottatt

    override fun avsender() = ARBEIDSGIVER

    override fun registrert() = mottatt // // dette tidsstempelet tilsvarer @opprettet fra spedisjon

    enum class Avsendersystem {
        NAV_NO,
        ALTINN,
        LPS
    }

    class Refusjon(
        private val beløp: Inntekt?,
        private val opphørsdato: LocalDate?,
        private val endringerIRefusjon: List<EndringIRefusjon> = emptyList()
    ) {
        internal fun leggTilRefusjon(
            refusjonshistorikk: Refusjonshistorikk,
            meldingsreferanseId: UUID,
            førsteFraværsdag: LocalDate?,
            arbeidsgiverperioder: List<Periode>
        ) {
            val refusjon = Refusjonshistorikk.Refusjon(meldingsreferanseId, førsteFraværsdag, arbeidsgiverperioder, beløp, opphørsdato, endringerIRefusjon.map { it.tilEndring() })
            refusjonshistorikk.leggTilRefusjon(refusjon)
        }

        class EndringIRefusjon(
            private val beløp: Inntekt,
            private val endringsdato: LocalDate
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

    internal fun ikkeHåndert(person: Person, vedtaksperioder: List<Vedtaksperiode>, sykmeldingsperioder: Sykmeldingsperioder, dager: DagerFraInntektsmelding) {
        if (håndtertNå(dager) || håndtertTidligere(vedtaksperioder)) return
        info("Inntektsmelding ikke håndtert")
        val overlappendeSykmeldingsperioder = sykmeldingsperioder.overlappendePerioder(dager)
        if (overlappendeSykmeldingsperioder.isNotEmpty()) {
            person.emitInntektsmeldingFørSøknadEvent(meldingsreferanseId(), overlappendeSykmeldingsperioder, organisasjonsnummer)
            return info("Inntektsmelding overlapper med sykmeldingsperioder $overlappendeSykmeldingsperioder")
        }
        person.emitInntektsmeldingIkkeHåndtert(this, organisasjonsnummer, dager.harPeriodeInnenfor16Dager(vedtaksperioder))
    }
    private fun håndtertNå(dager: DagerFraInntektsmelding) = håndtertInntekt || dager.noenDagerHåndtert()
    private fun håndtertTidligere(vedtaksperioder: List<Vedtaksperiode>) = vedtaksperioder.any { meldingsreferanseId() in it.hendelseIder() }
    internal fun jurist(jurist: MaskinellJurist) = jurist.medInntektsmelding(this.meldingsreferanseId())
    internal fun skalIkkeOppdatereVilkårsgrunnlag(sykdomstidslinjeperiode: Periode?) =
        sykdomstidslinjeperiode != null && beregnetInntektsdato !in sykdomstidslinjeperiode && inntektsdato == null
}
