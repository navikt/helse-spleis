package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.forrigeDag
import no.nav.helse.førsteArbeidsdag
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.hendelser.Periode.Companion.periode
import no.nav.helse.hendelser.inntektsmelding.DagerFraInntektsmelding
import no.nav.helse.nesteDag
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.InntektsmeldingInfo
import no.nav.helse.person.Person
import no.nav.helse.person.Sykmeldingsperioder
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_22
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_23
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_3
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_7
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_8
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.inntekt.Inntektshistorikk
import no.nav.helse.person.inntekt.Inntektsmelding
import no.nav.helse.person.inntekt.Refusjonshistorikk
import no.nav.helse.person.inntekt.Refusjonshistorikk.Refusjon.EndringIRefusjon.Companion.refusjonsopplysninger
import no.nav.helse.person.inntekt.Sykepengegrunnlag.ArbeidsgiverInntektsopplysningerOverstyringer
import no.nav.helse.sykdomstidslinje.Dag.Companion.replace
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.merge
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class Inntektsmelding(
    meldingsreferanseId: UUID,
    private val refusjon: Refusjon,
    orgnummer: String,
    fødselsnummer: String,
    aktørId: String,
    private val førsteFraværsdag: LocalDate?,
    private val beregnetInntekt: Inntekt,
    arbeidsgiverperioder: List<Periode>,
    private val arbeidsforholdId: String?,
    private val begrunnelseForReduksjonEllerIkkeUtbetalt: String?,
    private val harOpphørAvNaturalytelser: Boolean = false,
    private val harFlereInntektsmeldinger: Boolean,
    mottatt: LocalDateTime,
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : SykdomstidslinjeHendelse(
    meldingsreferanseId = meldingsreferanseId,
    fødselsnummer = fødselsnummer,
    aktørId = aktørId,
    organisasjonsnummer = orgnummer,
    opprettet = mottatt, aktivitetslogg = aktivitetslogg
) {
    private val arbeidsgiverperioder = arbeidsgiverperioder.grupperSammenhengendePerioder()
    private val arbeidsgiverperiode = this.arbeidsgiverperioder.periode()
    private val overlappsperiode = when {
        // første fraværsdag er oppgitt etter arbeidsgiverperioden
        arbeidsgiverperiode == null || førsteFraværsdagErEtterArbeidsgiverperioden(førsteFraværsdag) -> førsteFraværsdag?.somPeriode()
        // kant-i-kant
        førsteFraværsdag?.forrigeDag == arbeidsgiverperiode.endInclusive -> arbeidsgiverperiode.oppdaterTom(arbeidsgiverperiode.endInclusive.nesteDag)
        else -> arbeidsgiverperiode
    }
    private var sykdomstidslinje: Sykdomstidslinje
    init {
        if (arbeidsgiverperioder.isEmpty() && førsteFraværsdag == null) logiskFeil("Arbeidsgiverperiode er tom og førsteFraværsdag er null")
        sykdomstidslinje = arbeidsgivertidslinje()
    }

    private var håndtertInntekt = false
    private val inntektsdato = if (førsteFraværsdagErEtterArbeidsgiverperioden(førsteFraværsdag)) førsteFraværsdag else this.arbeidsgiverperioder.maxOf { it.start }

    private companion object {
        private val ikkeStøttedeBegrunnelserForReduksjon = setOf(
            "BetvilerArbeidsufoerhet",
            "FiskerMedHyre",
            "StreikEllerLockout",
            "FravaerUtenGyldigGrunn",
            "BeskjedGittForSent",
            "IkkeLoenn"
        )
    }

    private fun arbeidsgivertidslinje(): Sykdomstidslinje {
        if (ignorerDager) return Sykdomstidslinje()
        val periodeMellom = førsteFraværsdag?.let{
            arbeidsgiverperiode?.periodeMellom(it)
        }
        if (periodeMellom != null && periodeMellom.count() >= 20) return Sykdomstidslinje()
        val arbeidsdager = arbeidsgiverperiode?.let { Sykdomstidslinje.arbeidsdager(arbeidsgiverperiode, kilde) } ?: Sykdomstidslinje()
        val friskHelg = førsteFraværsdag
            ?.takeIf { arbeidsgiverperiode?.erRettFør(førsteFraværsdag) == true }
            ?.let { arbeidsgiverperiode?.periodeMellom(førsteFraværsdag) }
            ?.let { Sykdomstidslinje.arbeidsdager(it, kilde) }
            ?: Sykdomstidslinje()
        return arbeidsdager.merge(lagArbeidsgivertidslinje(), replace).merge(friskHelg)
    }

    private val ignorerDager get(): Boolean {
        if (begrunnelseForReduksjonEllerIkkeUtbetalt.isNullOrBlank()) return false
        if (begrunnelseForReduksjonEllerIkkeUtbetalt in ikkeStøttedeBegrunnelserForReduksjon) return true
        if (arbeidsgiverperioder.size > 1) return true
        return false
    }

    private fun lagArbeidsgivertidslinje(): Sykdomstidslinje {
        check(!ignorerDager) { "Skal ikke brukes når vi ignorerer dager fra inntektsmelding" }
        if (begrunnelseForReduksjonEllerIkkeUtbetalt.isNullOrBlank()) return arbeidsgiverperioder.map(::arbeidsgiverdager).merge()
        return arbeidsgiverperiodeNavSkalUtbetale().map(::sykedagerNav).merge()
    }

    private fun arbeidsgiverperiodeNavSkalUtbetale(): List<Periode> {
        if (arbeidsgiverperioder.isNotEmpty()) return arbeidsgiverperioder
        return listOfNotNull(førsteFraværsdag?.førsteArbeidsdag()?.somPeriode())
    }

    private fun arbeidsgiverdager(periode: Periode) = Sykdomstidslinje.arbeidsgiverdager(periode.start, periode.endInclusive, 100.prosent, kilde)
    private fun sykedagerNav(periode: Periode) = Sykdomstidslinje.sykedagerNav(periode.start, periode.endInclusive, 100.prosent, kilde)

    override fun sykdomstidslinje() = sykdomstidslinje

    // Pad days prior to employer-paid days with assumed work days
    override fun padLeft(dato: LocalDate) {
        check(dato > LocalDate.MIN)
        if (arbeidsgiverperioder.isEmpty()) return  // No justification to pad
        val førsteDag = sykdomstidslinje.førsteDag()
        if (dato >= førsteDag) return  // No need to pad if sykdomstidslinje early enough
        sykdomstidslinje += Sykdomstidslinje.arbeidsdager(dato, førsteDag.minusDays(1), this.kilde)
    }

    internal fun overlapperMed(other: Periode) = overlappsperiode?.let { other.overlapperMed(it) } ?: false

    override fun overlappsperiode() = sykdomstidslinje.periode() ?: førsteFraværsdag?.somPeriode()

    @OptIn(ExperimentalContracts::class)
    private fun førsteFraværsdagErEtterArbeidsgiverperioden(førsteFraværsdag: LocalDate?): Boolean {
        contract {
            returns(true) implies (førsteFraværsdag != null)
        }
        if (førsteFraværsdag == null) return false
        return arbeidsgiverperiode == null || førsteFraværsdag > arbeidsgiverperiode.endInclusive.nesteDag
    }

    internal fun validerArbeidsgiverperiode(periode: Periode, arbeidsgiverperiode: Arbeidsgiverperiode?): IAktivitetslogg {
        if (!skalValideresAv(periode)) return this
        if (arbeidsgiverperiode != null) validerArbeidsgiverperiode(arbeidsgiverperiode)
        if (arbeidsgiverperioder.isEmpty()) info("Inntektsmeldingen mangler arbeidsgiverperiode. Vurder om vilkårene for sykepenger er oppfylt, og om det skal være arbeidsgiverperiode")
        return this
    }

    internal fun skalValideresAv(periode: Periode) = overlappsperiode?.overlapperMed(periode) == true || ikkeUtbetaltAGPOgAGPOverlapper(periode)

    private fun ikkeUtbetaltAGPOgAGPOverlapper(periode: Periode) = begrunnelseForReduksjonEllerIkkeUtbetalt != null && arbeidsgiverperioder.periode()?.overlapperMed(periode) == true

    override fun valider(periode: Periode, subsumsjonObserver: SubsumsjonObserver): IAktivitetslogg {
        if (!skalValideresAv(periode)) return this
        if (harOpphørAvNaturalytelser) funksjonellFeil(RV_IM_7)
        if (harFlereInntektsmeldinger) varsel(RV_IM_22)
        if (begrunnelseForReduksjonEllerIkkeUtbetalt.isNullOrBlank()) return this
        info("Arbeidsgiver har redusert utbetaling av arbeidsgiverperioden på grunn av: %s".format(begrunnelseForReduksjonEllerIkkeUtbetalt))
        if (arbeidsgiverperioder.size > 1) funksjonellFeil(RV_IM_23)
        if (begrunnelseForReduksjonEllerIkkeUtbetalt in ikkeStøttedeBegrunnelserForReduksjon) funksjonellFeil(RV_IM_8)
        else varsel(RV_IM_8)
        return this
    }

    private fun validerArbeidsgiverperiode(arbeidsgiverperiode: Arbeidsgiverperiode) {
        if (arbeidsgiverperiode.sammenlign(arbeidsgiverperioder)) return
        varsel(RV_IM_3)
    }

    internal fun addInntekt(inntektshistorikk: Inntektshistorikk, alternativInntektsdato: LocalDate) {
        if (alternativInntektsdato == this.inntektsdato) return
        if (!inntektshistorikk.leggTil(Inntektsmelding(alternativInntektsdato, meldingsreferanseId(), beregnetInntekt))) return
        info("Lagrer inntekt på alternativ inntektsdato $alternativInntektsdato")
    }

    internal fun addInntekt(
        inntektshistorikk: Inntektshistorikk,
        subsumsjonObserver: SubsumsjonObserver,
        sykdomstidslinje: Sykdomstidslinje
    ): Pair<LocalDate, Boolean> {
        val sykdomstidslinjeperiode = sykdomstidslinje.sykdomsperiode()
        if (sykdomstidslinjeperiode == null || inntektsdato !in sykdomstidslinjeperiode) {
            info("Lagrer ikke inntekt på skjæringstidspunkt fordi inntektdato er oppgitt til å være utenfor den perioden arbeidsgiver har sykdom for")
            return inntektsdato to false
        }
        val (årligInntekt, dagligInntekt) = beregnetInntekt.reflection { årlig, _, daglig, _ -> årlig to daglig }
        subsumsjonObserver.`§ 8-10 ledd 3`(årligInntekt, dagligInntekt)
        val lagtTilNå = inntektshistorikk.leggTil(Inntektsmelding(inntektsdato, meldingsreferanseId(), beregnetInntekt))
        if (!lagtTilNå) info("Inntekt allerede lagret i inntektshistorikken på arbeidsgiver")
        return inntektsdato to lagtTilNå
    }

    internal fun leggTilRefusjon(refusjonshistorikk: Refusjonshistorikk) {
        refusjon.leggTilRefusjon(refusjonshistorikk, meldingsreferanseId(), førsteFraværsdag, arbeidsgiverperioder)
    }

    internal fun inntektsmeldingsinfo() = InntektsmeldingInfo(id = meldingsreferanseId(), arbeidsforholdId = arbeidsforholdId)

    override fun leggTil(hendelseIder: MutableSet<Dokumentsporing>): Boolean {
        håndtertInntekt = true
        return hendelseIder.add(Dokumentsporing.inntektsmeldingInntekt(meldingsreferanseId()))
    }


    internal fun nyeArbeidsgiverInntektsopplysninger(builder: ArbeidsgiverInntektsopplysningerOverstyringer, skjæringstidspunkt: LocalDate) {
        val refusjonshistorikk = Refusjonshistorikk()
        refusjon.leggTilRefusjon(refusjonshistorikk, meldingsreferanseId(), førsteFraværsdag, arbeidsgiverperioder)
        // startskuddet dikterer hvorvidt refusjonsopplysningene skal strekkes tilbake til å fylle gråsonen (perioden mellom skjæringstidspunkt og første refusjonsopplysning)
        // inntektsdato er den dagen refusjonsopplysningen i IM gjelder fom slik at det blir ingen strekking da, bare dersom skjæringstidspunkt brukes
        val startskudd = if (builder.ingenRefusjonsopplysninger(organisasjonsnummer)) skjæringstidspunkt else inntektsdato
        builder.leggTilInntekt(
            ArbeidsgiverInntektsopplysning(
                organisasjonsnummer,
                Inntektsmelding(inntektsdato, meldingsreferanseId(), beregnetInntekt),
                refusjonshistorikk.refusjonsopplysninger(startskudd, this)
            )
        )

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

    internal fun overlappendeSykmeldingsperioder(perioder: List<Periode>): List<Periode> {
        if (overlappsperiode == null) return emptyList()
        return perioder.mapNotNull { periode -> periode.overlappendePeriode(overlappsperiode) }
    }

    internal fun ikkeHåndert(person: Person, vedtaksperioder: List<Vedtaksperiode>, sykmeldingsperioder: Sykmeldingsperioder, dager: DagerFraInntektsmelding) {
        if (håndtertNå(dager) || håndtertTidligere(vedtaksperioder)) return
        info("Inntektsmelding ikke håndtert")
        val overlappendeSykmeldingsperioder = sykmeldingsperioder.overlappendePerioder(this)
        if (overlappendeSykmeldingsperioder.isNotEmpty()) {
            person.emitInntektsmeldingFørSøknadEvent(this, overlappendeSykmeldingsperioder, organisasjonsnummer)
            return info("Inntektsmelding overlapper med sykmeldingsperioder $overlappendeSykmeldingsperioder")
        }
        person.emitInntektsmeldingIkkeHåndtert(this, organisasjonsnummer)
    }

    private fun håndtertNå(dager: DagerFraInntektsmelding) = håndtertInntekt || dager.noenDagerHåndtert()
    private fun håndtertTidligere(vedtaksperioder: List<Vedtaksperiode>) = vedtaksperioder.any { meldingsreferanseId() in it.hendelseIder() }
}
