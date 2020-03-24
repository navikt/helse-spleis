package no.nav.helse.hendelser

import no.nav.helse.hendelser.Inntektsmelding.InntektsmeldingPeriode.Arbeidsgiverperiode
import no.nav.helse.hendelser.Inntektsmelding.InntektsmeldingPeriode.Ferieperiode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Inntekthistorikk
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.NySykdomstidslinje
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

    private val arbeidsgiverperioder: List<Arbeidsgiverperiode>
    private val ferieperioder: List<Ferieperiode>
    private var forrigeTom: LocalDate? = null
    private var sykdomstidslinje: ConcreteSykdomstidslinje
    private var nySykdomstidslinje: NySykdomstidslinje

    init {
        this.arbeidsgiverperioder =
            arbeidsgiverperioder.sortedBy { it.start }.map { Arbeidsgiverperiode(it.start, it.endInclusive) }
        this.ferieperioder = ferieperioder.map { Ferieperiode(it.start, it.endInclusive) }
        val old_førsteFraværsdagtidslinje =
            ConcreteSykdomstidslinje.egenmeldingsdag(førsteFraværsdag, InntektsmeldingDagFactory)
        val old_arbeidsgiverperiodetidslinje = this.arbeidsgiverperioder
            .map { it.sykdomstidslinje(this) }
            .takeUnless { it.isEmpty() }
            ?.merge(IdentiskDagTurnering) {
                ConcreteSykdomstidslinje.ikkeSykedag(it, InntektsmeldingDagFactory)
            } ?: old_førsteFraværsdagtidslinje

        val old_ferieperiodetidslinje = this.ferieperioder
            .map { it.sykdomstidslinje(this) }
            .takeUnless { it.isEmpty() }
            ?.merge(IdentiskDagTurnering)

        val old_inntektsmeldingtidslinje =
            old_ferieperiodetidslinje?.let { old_arbeidsgiverperiodetidslinje.merge(it, InntektsmeldingTurnering) }
                ?: old_arbeidsgiverperiodetidslinje

        this.sykdomstidslinje = old_inntektsmeldingtidslinje.let {
            if (old_arbeidsgiverperiodetidslinje.overlapperMed(old_førsteFraværsdagtidslinje)) it else it.merge(
                old_førsteFraværsdagtidslinje,
                InntektsmeldingTurnering
            )
        }

        val førsteFraværsdagtidslinje =
            NySykdomstidslinje.egenmeldingsdager(førsteFraværsdag, førsteFraværsdag, InntektsmeldingDagFactory)

        val arbeidsgiverperiodetidslinje = this.arbeidsgiverperioder
            .map { it.nySykdomstidslinje(this) }
            .takeUnless { it.isEmpty() }
            ?.merge(IdentiskDagTurnering) {
                NySykdomstidslinje.ikkeSykedag(it, InntektsmeldingDagFactory)
            } ?: førsteFraværsdagtidslinje

        val ferieperiodetidslinje = this.ferieperioder
            .map { it.nySykdomstidslinje(this) }
            .takeUnless { it.isEmpty() }
            ?.merge(IdentiskDagTurnering)

        val inntektsmeldingtidslinje =
            ferieperiodetidslinje?.let { arbeidsgiverperiodetidslinje.merge(it, InntektsmeldingTurnering) }
                ?: arbeidsgiverperiodetidslinje

        this.nySykdomstidslinje = inntektsmeldingtidslinje.let {
            if (arbeidsgiverperiodetidslinje.overlapperMed(førsteFraværsdagtidslinje)) it else it.merge(
                førsteFraværsdagtidslinje,
                InntektsmeldingTurnering
            )
        }
    }

    override fun sykdomstidslinje() = sykdomstidslinje

    override fun nySykdomstidslinje() = nySykdomstidslinje

    override fun sykdomstidslinje(tom: LocalDate): ConcreteSykdomstidslinje {
        require(forrigeTom == null || (forrigeTom != null && tom > forrigeTom)) { "Kalte metoden flere ganger med samme eller en tidligere dato" }

        val subsetFom = forrigeTom?.plusDays(1)
        return sykdomstidslinje().subset(subsetFom, tom)
            .also { trimLeft(tom) }
            ?: severe("Ugyldig subsetting av tidslinjen til inntektsmeldingen: [$subsetFom, $tom] gav en tom tidslinje")
    }

    override fun nySykdomstidslinje(tom: LocalDate): NySykdomstidslinje {
        require(forrigeTom == null || (forrigeTom != null && tom > forrigeTom)) { "Kalte metoden flere ganger med samme eller en tidligere dato" }

        val subsetFom = forrigeTom?.plusDays(1)
        return nySykdomstidslinje().subset(subsetFom, tom)
            .also { trimLeft(tom) }
            ?: severe("Ugyldig subsetting av tidslinjen til inntektsmeldingen: [$subsetFom, $tom] gav en tom tidslinje")
    }

    internal fun trimLeft(dato: LocalDate) {
        forrigeTom = dato
    }

    // Pad days prior to employer-paid days with assumed work days
    override fun old_padLeft(dato: LocalDate) {
        if (arbeidsgiverperioder.isEmpty()) return  // No justification to pad
        if (dato >= sykdomstidslinje.førsteDag()) return  // No need to pad if sykdomstidslinje early enough
        sykdomstidslinje = sykdomstidslinje.join(ConcreteSykdomstidslinje.ikkeSykedager(dato, sykdomstidslinje.førsteDag().minusDays(1), InntektsmeldingDagFactory))
    }

    // Pad days prior to employer-paid days with assumed work days
    override fun padLeft(dato: LocalDate) {
        if (arbeidsgiverperioder.isEmpty()) return  // No justification to pad
        if (dato >= nySykdomstidslinje.førsteDag()) return  // No need to pad if sykdomstidslinje early enough
        nySykdomstidslinje = nySykdomstidslinje.plus(NySykdomstidslinje.ikkeSykedager(
            dato,
            sykdomstidslinje.førsteDag().minusDays(1),
            InntektsmeldingDagFactory
        ))
    }

    override fun valider(): Aktivitetslogg {
        if (!ingenOverlappende()) aktivitetslogg.error("Inntektsmelding inneholder arbeidsgiverperioder eller ferieperioder som overlapper med hverandre")
        if (refusjon == null) aktivitetslogg.error("Arbeidsgiver forskutterer ikke (krever ikke refusjon)")
        else if (refusjon.beløpPrMåned != beregnetInntekt) aktivitetslogg.error("Inntektsmelding inneholder beregnet inntekt og refusjon som avviker med hverandre")
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

    private fun ingenOverlappende() = (arbeidsgiverperioder + ferieperioder)
        .sortedBy { it.fom }
        .zipWithNext(InntektsmeldingPeriode::ingenOverlappende)
        .all { it }

    internal fun addInntekt(inntekthistorikk: Inntekthistorikk) {
        inntekthistorikk.add(
            førsteFraværsdag.minusDays(1),  // Assuming salary is the day before the first sykedag
            meldingsreferanseId(),
            beregnetInntekt.toBigDecimal()
        )
    }

    class Refusjon(
        val opphørsdato: LocalDate?,
        val beløpPrMåned: Double,
        val endringerIRefusjon: List<LocalDate> = emptyList()
    )

    sealed class InntektsmeldingPeriode(
        internal val fom: LocalDate,
        internal val tom: LocalDate
    ) {

        internal abstract fun sykdomstidslinje(inntektsmelding: Inntektsmelding): ConcreteSykdomstidslinje

        internal abstract fun nySykdomstidslinje(inntektsmelding: Inntektsmelding): NySykdomstidslinje

        internal fun ingenOverlappende(other: InntektsmeldingPeriode) =
            maxOf(this.fom, other.fom) > minOf(this.tom, other.tom)

        class Arbeidsgiverperiode(fom: LocalDate, tom: LocalDate) : InntektsmeldingPeriode(fom, tom) {
            override fun sykdomstidslinje(inntektsmelding: Inntektsmelding) =
                ConcreteSykdomstidslinje.egenmeldingsdager(fom, tom, InntektsmeldingDagFactory)

            override fun nySykdomstidslinje(inntektsmelding: Inntektsmelding) =
                NySykdomstidslinje.egenmeldingsdager(fom, tom, InntektsmeldingDagFactory)

        }

        class Ferieperiode(fom: LocalDate, tom: LocalDate) : InntektsmeldingPeriode(fom, tom) {
            override fun sykdomstidslinje(inntektsmelding: Inntektsmelding) =
                ConcreteSykdomstidslinje.ferie(fom, tom, InntektsmeldingDagFactory)

            override fun nySykdomstidslinje(inntektsmelding: Inntektsmelding) =
                NySykdomstidslinje.ferie(fom, tom, InntektsmeldingDagFactory)
        }
    }

    internal object InntektsmeldingDagFactory : DagFactory {
        override fun arbeidsdag(dato: LocalDate): Arbeidsdag = Arbeidsdag.Inntektsmelding(dato)
        override fun egenmeldingsdag(dato: LocalDate): Egenmeldingsdag = Egenmeldingsdag.Inntektsmelding(dato)
        override fun feriedag(dato: LocalDate): Feriedag = Feriedag.Inntektsmelding(dato)
    }

    private object InntektsmeldingTurnering : Dagturnering {
        override fun beste(venstre: Dag, høyre: Dag): Dag {
            return when {
                venstre is ImplisittDag -> høyre
                høyre is ImplisittDag -> venstre
                venstre is Feriedag.Inntektsmelding && høyre is Arbeidsdag.Inntektsmelding -> venstre
                høyre is Feriedag.Inntektsmelding && venstre is Arbeidsdag.Inntektsmelding -> høyre
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
