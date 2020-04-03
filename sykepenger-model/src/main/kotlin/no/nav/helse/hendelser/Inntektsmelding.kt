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
    private val refusjon: Refusjon?,
    private val orgnummer: String,
    private val fødselsnummer: String,
    private val aktørId: String,
    internal val førsteFraværsdag: LocalDate,
    internal val beregnetInntekt: Double,
    arbeidsgiverperioder: List<Periode>,
    ferieperioder: List<Periode>
) : SykdomstidslinjeHendelse(meldingsreferanseId) {

    private var beingQualified = false
    private val arbeidsgiverperioder: List<Arbeidsgiverperiode>
    private val ferieperioder: List<Ferieperiode>
    private var forrigeTom: LocalDate? = null
    private var sykdomstidslinje: Sykdomstidslinje

    init {
        this.arbeidsgiverperioder =
            arbeidsgiverperioder.sortedBy { it.start }.map { Arbeidsgiverperiode(it.start, it.endInclusive) }
        this.ferieperioder = ferieperioder.map { Ferieperiode(it.start, it.endInclusive) }

        val førsteFraværsdagtidslinje =
            Sykdomstidslinje.egenmeldingsdager(førsteFraværsdag, førsteFraværsdag, InntektsmeldingDagFactory)

        val arbeidsgiverperiodetidslinje = this.arbeidsgiverperioder
            .map { it.sykdomstidslinje(this) }
            .takeUnless { it.isEmpty() }
            ?.merge(IdentiskDagTurnering) {
                Sykdomstidslinje.ikkeSykedag(it, InntektsmeldingDagFactory)
            } ?: førsteFraværsdagtidslinje

        val ferieperiodetidslinje = this.ferieperioder
            .map { it.sykdomstidslinje(this) }
            .takeUnless { it.isEmpty() }
            ?.merge(IdentiskDagTurnering)

        val inntektsmeldingtidslinje =
            ferieperiodetidslinje?.let { arbeidsgiverperiodetidslinje.merge(it, InntektsmeldingTurnering) }
                ?: arbeidsgiverperiodetidslinje

        this.sykdomstidslinje = inntektsmeldingtidslinje.let {
            if (arbeidsgiverperiodetidslinje.overlapperMed(førsteFraværsdagtidslinje)) it else it.merge(
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
        if (refusjon == null) aktivitetslogg.error("Arbeidsgiver forskutterer ikke (krever ikke refusjon)")
        else if (refusjon.beløpPrMåned != beregnetInntekt) aktivitetslogg.error("Inntektsmelding inneholder beregnet inntekt og refusjon som avviker med hverandre")
        if (arbeidsgiverperioder.isEmpty()) aktivitetslogg.warn("Inntektsmelding inneholder ikke arbeidsgiverperiode, dette må sjekkes manuelt")
        return aktivitetslogg
    }

    override fun aktørId() = aktørId

    override fun fødselsnummer() = fødselsnummer

    override fun organisasjonsnummer() = orgnummer

    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) = arbeidsgiver.håndter(this)

    internal fun harEndringIRefusjon(sisteUtbetalingsdag: LocalDate): Boolean {
        if (refusjon == null) return false
        refusjon.opphørsdato?.also { if (it <= sisteUtbetalingsdag) return true }
        return refusjon.endringerIRefusjon.any { it <= sisteUtbetalingsdag }
    }

    internal fun addInntekt(inntekthistorikk: Inntekthistorikk) {
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
        val opphørsdato: LocalDate?,
        val beløpPrMåned: Double,
        val endringerIRefusjon: List<LocalDate> = emptyList()
    )

    sealed class InntektsmeldingPeriode(
        internal val fom: LocalDate,
        internal val tom: LocalDate
    ) {

        internal abstract fun sykdomstidslinje(inntektsmelding: Inntektsmelding): Sykdomstidslinje

        class Arbeidsgiverperiode(fom: LocalDate, tom: LocalDate) : InntektsmeldingPeriode(fom, tom) {
            override fun sykdomstidslinje(inntektsmelding: Inntektsmelding) =
                Sykdomstidslinje.egenmeldingsdager(fom, tom, InntektsmeldingDagFactory)

        }

        class Ferieperiode(fom: LocalDate, tom: LocalDate) : InntektsmeldingPeriode(fom, tom) {
            override fun sykdomstidslinje(inntektsmelding: Inntektsmelding) =
                Sykdomstidslinje.ferie(fom, tom, InntektsmeldingDagFactory)
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
