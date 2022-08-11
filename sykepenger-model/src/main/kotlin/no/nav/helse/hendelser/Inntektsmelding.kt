package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.forrigeDag
import no.nav.helse.hendelser.Inntektsmelding.Refusjon.EndringIRefusjon.Companion.cacheRefusjon
import no.nav.helse.hendelser.Inntektsmelding.Refusjon.EndringIRefusjon.Companion.endrerRefusjon
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.hendelser.Periode.Companion.periode
import no.nav.helse.nesteDag
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.InntektsmeldingInfo
import no.nav.helse.person.Personopplysninger
import no.nav.helse.person.Refusjonshistorikk
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.somFødselsnummer
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
    private val fødselsdato: LocalDate,
    private val førsteFraværsdag: LocalDate?,
    private val beregnetInntekt: Inntekt,
    arbeidsgiverperioder: List<Periode>,
    private val arbeidsforholdId: String?,
    private val begrunnelseForReduksjonEllerIkkeUtbetalt: String?,
    private val harOpphørAvNaturalytelser: Boolean = false,
    mottatt: LocalDateTime
) : SykdomstidslinjeHendelse(meldingsreferanseId, fødselsnummer, aktørId, orgnummer, mottatt) {
    internal companion object {
        internal const val WARN_UENIGHET_ARBEIDSGIVERPERIODE = "Inntektsmeldingen og vedtaksløsningen er uenige om beregningen av arbeidsgiverperioden. Undersøk hva som er riktig arbeidsgiverperiode."
        internal const val WARN_ULIKHET_FØRSTE_FRAVÆRSDAG_OG_SKJÆRINGSTIDSPUNKT = "Første fraværsdag i inntektsmeldingen er ulik skjæringstidspunktet. Kontrollér at inntektsmeldingen er knyttet til riktig periode."
    }

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
        if (arbeidsgiverperioder.isEmpty() && førsteFraværsdag == null) severe("Arbeidsgiverperiode er tom og førsteFraværsdag er null")
        sykdomstidslinje = arbeidsgivertidslinje()
    }

    override fun personopplysninger() = Personopplysninger(fødselsnummer.somFødselsnummer(), aktørId, fødselsdato)

    private fun arbeidsgivertidslinje(): Sykdomstidslinje {
        val arbeidsdager = arbeidsgiverperiode?.let { Sykdomstidslinje.arbeidsdager(arbeidsgiverperiode, kilde) } ?: return Sykdomstidslinje()
        val friskHelg = førsteFraværsdag
            ?.takeIf { arbeidsgiverperiode.erRettFør(førsteFraværsdag) }
            ?.let { arbeidsgiverperiode.periodeMellom(førsteFraværsdag) }
            ?.let { Sykdomstidslinje.arbeidsdager(it, kilde) }
            ?: Sykdomstidslinje()
        val arbeidsgiverdager = arbeidsgiverperioder.map(::asArbeidsgivertidslinje).merge()
        return arbeidsdager.merge(arbeidsgiverdager, replace).merge(friskHelg)
    }

    private fun asArbeidsgivertidslinje(periode: Periode) = Sykdomstidslinje.arbeidsgiverdager(periode.start, periode.endInclusive, 100.prosent, kilde)

    override fun sykdomstidslinje() = sykdomstidslinje

    // Pad days prior to employer-paid days with assumed work days
    override fun padLeft(dato: LocalDate) {
        check(dato > LocalDate.MIN)
        if (arbeidsgiverperioder.isEmpty()) return  // No justification to pad
        val førsteDag = sykdomstidslinje.førsteDag()
        if (dato >= førsteDag) return  // No need to pad if sykdomstidslinje early enough
        sykdomstidslinje += Sykdomstidslinje.arbeidsdager(dato, førsteDag.minusDays(1), this.kilde)
    }

    override fun overlappsperiode() = overlappsperiode

    internal fun erRelevant(periode: Periode, perioder: List<Periode>): Boolean {
        val relevantePerioder = perioder.dropWhile { !erRelevant(it) }
        if (relevantePerioder.isEmpty()) return false

        padLeft(periode.start)
        if (periode !in relevantePerioder) {
            trimLeft(periode.endInclusive)
            return false
        }

        if (førsteFraværsdagErEtterArbeidsgiverperioden(førsteFraværsdag) && perioder.size != relevantePerioder.size)
            warn("Vi har mottatt en inntektsmelding i en løpende sykmeldingsperiode med oppgitt første/bestemmende fraværsdag som er ulik tidligere fastsatt skjæringstidspunkt.")
        return true
    }

    @OptIn(ExperimentalContracts::class)
    private fun førsteFraværsdagErEtterArbeidsgiverperioden(førsteFraværsdag: LocalDate?): Boolean {
        contract {
            returns(true) implies (førsteFraværsdag != null)
        }
        if (førsteFraværsdag == null) return false
        return arbeidsgiverperiode == null || førsteFraværsdag > arbeidsgiverperiode.endInclusive.nesteDag
    }

    internal fun valider(periode: Periode, skjæringstidspunkt: LocalDate, arbeidsgiverperiode: Arbeidsgiverperiode?, subsumsjonObserver: SubsumsjonObserver): IAktivitetslogg {
        validerFørsteFraværsdag(skjæringstidspunkt)
        if (arbeidsgiverperiode != null) validerArbeidsgiverperiode(arbeidsgiverperiode)
        return valider(periode, subsumsjonObserver)
    }

    override fun valider(periode: Periode, subsumsjonObserver: SubsumsjonObserver): IAktivitetslogg {
        refusjon.valider(this, periode, beregnetInntekt)
        if (arbeidsgiverperioder.isEmpty()) info("Inntektsmeldingen mangler arbeidsgiverperiode. Vurder om vilkårene for sykepenger er oppfylt, og om det skal være arbeidsgiverperiode")
        begrunnelseForReduksjonEllerIkkeUtbetalt?.takeIf(String::isNotBlank)?.also {
            error(
                "Arbeidsgiver har redusert utbetaling av arbeidsgiverperioden på grunn av: %s",
                it
            )
        }
        if (harOpphørAvNaturalytelser) error("Brukeren har opphold i naturalytelser")
        return this
    }

    private fun validerFørsteFraværsdag(skjæringstidspunkt: LocalDate) {
        if (førsteFraværsdag == null || førsteFraværsdag == skjæringstidspunkt) return
        warn(WARN_ULIKHET_FØRSTE_FRAVÆRSDAG_OG_SKJÆRINGSTIDSPUNKT)
    }

    private fun validerArbeidsgiverperiode(arbeidsgiverperiode: Arbeidsgiverperiode) {
        if (førsteFraværsdagErEtterArbeidsgiverperioden(førsteFraværsdag) || arbeidsgiverperiode.sammenlign(arbeidsgiverperioder)) return
        warn(WARN_UENIGHET_ARBEIDSGIVERPERIODE)
    }

    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) = arbeidsgiver.håndter(this)

    private var inntektLagret = false
    internal fun addInntekt(inntektshistorikk: Inntektshistorikk, førsteFraværsdagFraSpleis: LocalDate, subsumsjonObserver: SubsumsjonObserver) {
        if (inntektLagret) return
        inntektLagret = true

        val inntektsdato = if (førsteFraværsdagErEtterArbeidsgiverperioden(førsteFraværsdag)) minOf(førsteFraværsdagFraSpleis, førsteFraværsdag) else arbeidsgiverperioder.maxOf { it.start }
        if (inntektsdato != førsteFraværsdag) {
            warn(WARN_ULIKHET_FØRSTE_FRAVÆRSDAG_OG_SKJÆRINGSTIDSPUNKT)
        }

        val (årligInntekt, dagligInntekt) = beregnetInntekt.reflection { årlig, _, daglig, _ -> årlig to daglig }
        subsumsjonObserver.`§ 8-10 ledd 3`(årligInntekt, dagligInntekt)
        inntektshistorikk.append {
            addInntektsmelding(
                inntektsdato,
                meldingsreferanseId(),
                beregnetInntekt
            )
        }
    }

    internal fun cacheRefusjon(refusjonshistorikk: Refusjonshistorikk) {
        refusjon.cacheRefusjon(refusjonshistorikk, meldingsreferanseId(), førsteFraværsdag, arbeidsgiverperioder)
    }

    internal fun inntektsmeldingsinfo() = InntektsmeldingInfo(id = meldingsreferanseId(), arbeidsforholdId = arbeidsforholdId)

    override fun leggTil(hendelseIder: MutableSet<Dokumentsporing>) {
        hendelseIder.add(Dokumentsporing.inntektsmelding(meldingsreferanseId()))
    }

    class Refusjon(
        private val beløp: Inntekt?,
        private val opphørsdato: LocalDate?,
        private val endringerIRefusjon: List<EndringIRefusjon> = emptyList()
    ) {
        class EndringIRefusjon(
            private val beløp: Inntekt,
            private val endringsdato: LocalDate
        ) {
            internal companion object {
                internal fun List<EndringIRefusjon>.endrerRefusjon(periode: Periode) =
                    any { it.endringsdato in periode }

                internal fun List<EndringIRefusjon>.minOf(opphørsdato: LocalDate?) =
                    (map { it.endringsdato } + opphørsdato).filterNotNull().minOrNull()

                internal fun List<EndringIRefusjon>.cacheRefusjon(
                    refusjonshistorikk: Refusjonshistorikk,
                    meldingsreferanseId: UUID,
                    førsteFraværsdag: LocalDate?,
                    arbeidsgiverperioder: List<Periode>,
                    beløp: Inntekt?,
                    sisteRefusjonsdag: LocalDate?
                ) {
                    refusjonshistorikk.leggTilRefusjon(
                        Refusjonshistorikk.Refusjon(
                            meldingsreferanseId = meldingsreferanseId,
                            førsteFraværsdag = førsteFraværsdag,
                            arbeidsgiverperioder = arbeidsgiverperioder,
                            beløp = beløp,
                            sisteRefusjonsdag = sisteRefusjonsdag,
                            endringerIRefusjon = map {
                                Refusjonshistorikk.Refusjon.EndringIRefusjon(
                                    it.beløp, it.endringsdato
                                )
                            }
                        )
                    )
                }
            }
        }

        internal fun valider(
            aktivitetslogg: IAktivitetslogg,
            periode: Periode,
            beregnetInntekt: Inntekt
        ): IAktivitetslogg {
            when {
                beregnetInntekt <= Inntekt.INGEN -> aktivitetslogg.error("Inntektsmelding inneholder ikke beregnet inntekt")
                (beløp == null || beløp <= Inntekt.INGEN) -> aktivitetslogg.info("Arbeidsgiver forskutterer ikke (krever ikke refusjon)")
                beløp != beregnetInntekt -> aktivitetslogg.info("Inntektsmelding inneholder beregnet inntekt og refusjon som avviker med hverandre")
                opphørerRefusjon(periode) -> aktivitetslogg.info("Arbeidsgiver opphører refusjon i perioden")
                opphørsdato != null -> aktivitetslogg.info("Arbeidsgiver opphører refusjon")
                endrerRefusjon(periode) -> aktivitetslogg.info("Arbeidsgiver endrer refusjon i perioden")
                endringerIRefusjon.isNotEmpty() -> aktivitetslogg.info("Arbeidsgiver har endringer i refusjon")
            }
            return aktivitetslogg
        }

        private fun opphørerRefusjon(periode: Periode) =
            opphørsdato?.let { it in periode } ?: false

        private fun endrerRefusjon(periode: Periode) =
            endringerIRefusjon.endrerRefusjon(periode)

        internal fun cacheRefusjon(
            refusjonshistorikk: Refusjonshistorikk,
            meldingsreferanseId: UUID,
            førsteFraværsdag: LocalDate?,
            arbeidsgiverperioder: List<Periode>
        ) {
            endringerIRefusjon.cacheRefusjon(refusjonshistorikk, meldingsreferanseId, førsteFraværsdag, arbeidsgiverperioder, beløp, opphørsdato)
        }
    }
}
