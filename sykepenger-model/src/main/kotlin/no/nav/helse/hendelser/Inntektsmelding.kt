package no.nav.helse.hendelser

import no.nav.helse.hendelser.Inntektsmelding.InntektsmeldingPeriode.Arbeidsgiverperiode
import no.nav.helse.hendelser.Inntektsmelding.InntektsmeldingPeriode.Ferieperiode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Inntekthistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.sykdomstidslinje.merge
import no.nav.helse.tournament.Dagturnering
import java.time.LocalDate
import java.util.*

class Inntektsmelding(
    meldingsreferanseId: UUID,
    private val refusjon: Refusjon,
    private val orgnummer: String,
    private val fødselsnummer: String,
    private val aktørId: String,
    internal val førsteFraværsdag: LocalDate?,
    internal val beregnetInntekt: Double,
    arbeidsgiverperioder: List<Periode>,
    ferieperioder: List<Periode>,
    private val arbeidsforholdId: String?,
    private val begrunnelseForReduksjonEllerIkkeUtbetalt: String?
) : SykdomstidslinjeHendelse(meldingsreferanseId) {

    private var beingQualified = false
    private val arbeidsgiverperioder: List<Arbeidsgiverperiode>
    private val ferieperioder: List<Ferieperiode>
    private var forrigeTom: LocalDate? = null
    private var sykdomstidslinje: Sykdomstidslinje

    init {
        this.arbeidsgiverperioder =
            arbeidsgiverperioder.sortedBy { it.start }.map { Arbeidsgiverperiode(it) }
        this.ferieperioder = ferieperioder.map { Ferieperiode(it) }

        val førsteFraværsdagtidslinje = førsteFraværsdag?.let { Sykdomstidslinje.egenmeldingsdager(it, it, InntektsmeldingDagFactory) }

        val arbeidsgiverperiodetidslinje = this.arbeidsgiverperioder
            .map { it.sykdomstidslinje(this) }
            .takeUnless { it.isEmpty() }
            ?.merge(IdentiskDagTurnering) {
                Sykdomstidslinje.ikkeSykedag(it, InntektsmeldingDagFactory)
            } ?: førsteFraværsdagtidslinje ?: severe("Arbeidsgiverperiode er tom og førsteFraværsdag er null")

        val ferieperiodetidslinje = this.ferieperioder
            .map { it.sykdomstidslinje(this) }
            .takeUnless { it.isEmpty() }
            ?.merge(IdentiskDagTurnering)

        val inntektsmeldingtidslinje =
            ferieperiodetidslinje?.let { arbeidsgiverperiodetidslinje.merge(it, InntektsmeldingTurnering) }
                ?: arbeidsgiverperiodetidslinje

        this.sykdomstidslinje = inntektsmeldingtidslinje.let {
            if (førsteFraværsdagtidslinje == null || arbeidsgiverperiodetidslinje.overlapperMed(førsteFraværsdagtidslinje)) it else it.merge(
                førsteFraværsdagtidslinje,
                InntektsmeldingTurnering
            )
        }
    }

    override fun sykdomstidslinje() = sykdomstidslinje

    override fun sykdomstidslinje(tom: LocalDate): Sykdomstidslinje {
        require(forrigeTom == null || (forrigeTom != null && tom > forrigeTom)) { "Kalte metoden flere ganger med samme eller en tidligere dato" }

        val subsetFom = forrigeTom?.plusDays(1)
        return sykdomstidslinje().subset(subsetFom, tom)
            .also { trimLeft(tom) }
            ?: severe("Ugyldig subsetting av tidslinjen til inntektsmeldingen: [$subsetFom, $tom] gav en tom tidslinje")
    }

    internal fun trimLeft(dato: LocalDate) {
        forrigeTom = dato
    }

    // Pad days prior to employer-paid days with assumed work days
    override fun padLeft(dato: LocalDate) {
        if (arbeidsgiverperioder.isEmpty()) return  // No justification to pad
        if (dato >= sykdomstidslinje.førsteDag()) return  // No need to pad if sykdomstidslinje early enough
        sykdomstidslinje = sykdomstidslinje.plus(Sykdomstidslinje.ikkeSykedager(
            dato,
            sykdomstidslinje.førsteDag().minusDays(1),
            InntektsmeldingDagFactory
        ))
    }

    override fun valider(): Aktivitetslogg {
        refusjon.valider(aktivitetslogg, beregnetInntekt)
        if (arbeidsgiverperioder.isEmpty()) aktivitetslogg.warn("Inntektsmelding inneholder ikke arbeidsgiverperiode")
        if (arbeidsforholdId != null && arbeidsforholdId.isNotBlank()) aktivitetslogg.warn("ArbeidsforholdsID fra inntektsmeldingen er utfylt")
        begrunnelseForReduksjonEllerIkkeUtbetalt?.takeIf(String::isNotBlank)?.also {
            aktivitetslogg.warn(
                "Arbeidsgiver har redusert utbetaling av arbeidsgiverperioden på grunn av: %s", it
            )
        }
        return aktivitetslogg
    }

    internal fun valider(periode: Periode): Aktivitetslogg {
        refusjon.valider(aktivitetslogg, periode)
        return aktivitetslogg
    }

    override fun aktørId() = aktørId

    override fun fødselsnummer() = fødselsnummer

    override fun organisasjonsnummer() = orgnummer

    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) = arbeidsgiver.håndter(this)

    internal fun addInntekt(inntekthistorikk: Inntekthistorikk) {
        if (førsteFraværsdag == null) return
        inntekthistorikk.add(
            førsteFraværsdag.minusDays(1),  // Assuming salary is the day before the first sykedag
            meldingsreferanseId(),
            beregnetInntekt.toBigDecimal()
        )
    }

    internal fun beingQualified() {
        beingQualified = true
    }

    fun isNotQualified() = !beingQualified

    class Refusjon(
        private val opphørsdato: LocalDate?,
        private val beløpPrMåned: Double?,
        private val endringerIRefusjon: List<LocalDate> = emptyList()
    ) {

        internal fun valider(aktivitetslogg: Aktivitetslogg, beregnetInntekt: Double): Aktivitetslogg {
            when {
                beløpPrMåned == null -> aktivitetslogg.error("Arbeidsgiver forskutterer ikke (krever ikke refusjon)")
                beløpPrMåned != beregnetInntekt -> aktivitetslogg.error("Inntektsmelding inneholder beregnet inntekt og refusjon som avviker med hverandre")
            }
            return aktivitetslogg
        }

        internal fun valider(aktivitetslogg: Aktivitetslogg, periode: Periode): Aktivitetslogg {
            when {
                opphørerRefusjon(periode) -> aktivitetslogg.error("Arbeidsgiver opphører refusjon i perioden")
                endrerRefusjon(periode) -> aktivitetslogg.error("Arbeidsgiver endrer refusjon i perioden")
            }
            return aktivitetslogg
        }

        private fun opphørerRefusjon(periode: Periode) =
            opphørsdato?.let { it in periode } ?: false

        private fun endrerRefusjon(periode: Periode) =
            endringerIRefusjon.any { it in periode }
    }

    private sealed class InntektsmeldingPeriode(
        protected val periode: Periode
    ) {

        internal abstract fun sykdomstidslinje(inntektsmelding: Inntektsmelding): Sykdomstidslinje

        class Arbeidsgiverperiode(periode: Periode) : InntektsmeldingPeriode(periode) {
            override fun sykdomstidslinje(inntektsmelding: Inntektsmelding) =
                Sykdomstidslinje.egenmeldingsdager(periode, InntektsmeldingDagFactory)

        }

        class Ferieperiode(periode: Periode) : InntektsmeldingPeriode(periode) {
            override fun sykdomstidslinje(inntektsmelding: Inntektsmelding) =
                Sykdomstidslinje.ferie(periode, InntektsmeldingDagFactory)
        }
    }

    internal object InntektsmeldingDagFactory : DagFactory {
        override fun arbeidsdag(dato: LocalDate): Arbeidsdag = Arbeidsdag.Inntektsmelding(dato)
        override fun egenmeldingsdag(dato: LocalDate): Egenmeldingsdag = Egenmeldingsdag.Inntektsmelding(dato)
        override fun feriedag(dato: LocalDate): Feriedag = Feriedag.Inntektsmelding(dato)
        override fun friskHelgedag(dato: LocalDate): FriskHelgedag = FriskHelgedag.Inntektsmelding(dato)
    }

    private object InntektsmeldingTurnering : Dagturnering {
        override fun beste(venstre: Dag, høyre: Dag): Dag {
            return when {
                venstre is ImplisittDag -> høyre
                høyre is ImplisittDag -> venstre
                venstre is Feriedag.Inntektsmelding && høyre is Arbeidsdag.Inntektsmelding -> venstre
                høyre is Feriedag.Inntektsmelding && venstre is Arbeidsdag.Inntektsmelding -> høyre
                venstre is Egenmeldingsdag.Inntektsmelding && høyre is Feriedag.Inntektsmelding -> venstre
                høyre is Egenmeldingsdag.Inntektsmelding && venstre is Feriedag.Inntektsmelding -> høyre
                venstre is FriskHelgedag.Inntektsmelding && høyre is Feriedag.Inntektsmelding -> høyre
                høyre is FriskHelgedag.Inntektsmelding && venstre is Feriedag.Inntektsmelding -> venstre
                else -> Ubestemtdag(venstre.dagen)
            }
        }
    }

    private object IdentiskDagTurnering : Dagturnering {
        override fun beste(venstre: Dag, høyre: Dag): Dag {
            return when {
                venstre::class == høyre::class -> venstre
                venstre is ImplisittDag -> høyre
                høyre is ImplisittDag -> venstre
                else -> Ubestemtdag(venstre.dagen)
            }
        }
    }
}
