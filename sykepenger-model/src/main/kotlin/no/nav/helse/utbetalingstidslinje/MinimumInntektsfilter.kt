package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import java.time.LocalDate

internal class MinimumInntektsfilter (
    private val alder: Alder,
    private val tidslinjer: List<Utbetalingstidslinje>,
    private val periode: Periode,
    private val aktivitetslogg: Aktivitetslogg
): Utbetalingstidslinje.UtbetalingsdagVisitor {

    private var inntekter = mutableMapOf<LocalDate, Double>()

    internal fun filter() {
        tidslinjer.forEach { it.accept(this) }
        val inntekterUnderMinimum = inntekter.filter { (dato, inntekt) -> inntekt < alder.minimumInntekt(dato) }.toMap()

        tidslinjer.forEach { it.avvis(inntekterUnderMinimum.keys.toList(), Begrunnelse.MinimumInntekt) }

        if (inntekterUnderMinimum.keys.toList() in periode)
            aktivitetslogg.warn("Avvist minst en dag som faller under minimum inntekt")
        else
            aktivitetslogg.info("Minimum inntekt har blitt sjekket uten problemer")
    }

    override fun visitNavDag(dag: NavDag) {
        addInntekt(dag)
    }

    override fun visitArbeidsdag(dag: Arbeidsdag) {
        addInntekt(dag)
    }

    override fun visitArbeidsgiverperiodeDag(dag: ArbeidsgiverperiodeDag) {
        addInntekt(dag)
    }

    override fun visitFridag(dag: Fridag) {
        addInntekt(dag)
    }

    private fun addInntekt(dag: Utbetalingstidslinje.Utbetalingsdag) {
        inntekter[dag.dato] = inntekter[dag.dato]?.plus(dag.inntekt) ?: dag.inntekt
    }

}
