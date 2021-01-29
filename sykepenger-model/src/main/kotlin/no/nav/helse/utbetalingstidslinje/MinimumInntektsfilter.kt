package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import no.nav.helse.økonomi.erUnderInntekstgrensen
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

internal class MinimumInntektsfilter(
    private val alder: Alder,
    private val tidslinjer: List<Utbetalingstidslinje>,
    private val periode: Periode,
    private val aktivitetslogg: IAktivitetslogg
) : UtbetalingsdagVisitor {

    private var økonomier = mutableMapOf<LocalDate, MutableMap<Utbetalingstidslinje.Utbetalingsdag, Økonomi>>()

    internal fun filter() {
        tidslinjer.forEach { it.accept(this) }
        val datoerUnderInntektsgrense = økonomier
            .filterValues { it.keys.any { dag -> dag is NavDag } }
            .mapValues { (_, value) -> value.values.toList() }
            .filter { (dato, økonomier) -> økonomier.erUnderInntekstgrensen(alder, dato) }
            .keys
            .toList()

        tidslinjer.forEach { it.avvis(datoerUnderInntektsgrense, Begrunnelse.MinimumInntekt) }

        if (datoerUnderInntektsgrense in periode)
            aktivitetslogg.warn("Inntekt under krav til minste sykepengegrunnlag. Vurder å sende brev")
        else
            aktivitetslogg.info("Krav til minste sykepengegrunnlag er oppfylt")
    }

    override fun visit(
        dag: NavDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        addInntekt(dato, dag, økonomi)
    }

    override fun visit(
        dag: Arbeidsdag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        addInntekt(dato, dag, økonomi)
    }

    override fun visit(
        dag: ArbeidsgiverperiodeDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        addInntekt(dato, dag, økonomi)
    }

    private fun addInntekt(dato: LocalDate, dag: Utbetalingstidslinje.Utbetalingsdag, økonomi: Økonomi) {
        økonomier.putIfAbsent(dato, mutableMapOf(dag to økonomi))?.put(dag, økonomi)
    }

}
