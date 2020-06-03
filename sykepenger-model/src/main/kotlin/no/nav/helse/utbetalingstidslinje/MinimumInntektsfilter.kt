package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.erUnderInntekstgrensen
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

internal class MinimumInntektsfilter(
    private val alder: Alder,
    private val tidslinjer: List<Utbetalingstidslinje>,
    private val periode: Periode,
    private val aktivitetslogg: Aktivitetslogg
) : UtbetalingsdagVisitor {

    private var økonomier = mutableMapOf<LocalDate, MutableList<Økonomi>>()

    internal fun filter() {
        tidslinjer.forEach { it.accept(this) }
        val datoerUnderInntektsgrense = økonomier
            .filter { (dato, økonomier) -> økonomier.erUnderInntekstgrensen(alder, dato) }
            .keys
            .toList()

        tidslinjer.forEach { it.avvis(datoerUnderInntektsgrense, Begrunnelse.MinimumInntekt) }

        if (datoerUnderInntektsgrense in periode)
            aktivitetslogg.warn("Inntekt under krav til minste sykepengegrunnlag")
        else
            aktivitetslogg.info("Krav til minste sykepengegrunnlag er oppfylt")
    }

    override fun visit(
        dag: NavDag,
        dato: LocalDate,
        økonomi: Økonomi,
        grad: Prosentdel,
        aktuellDagsinntekt: Double?,
        dekningsgrunnlag: Double?,
        arbeidsgiverbeløp: Int?,
        personbeløp: Int?
    ) {
        addInntekt(dato, økonomi)
    }

    override fun visit(
        dag: Arbeidsdag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        addInntekt(dato, økonomi)
    }

    override fun visit(
        dag: ArbeidsgiverperiodeDag,
        dato: LocalDate,
        økonomi: Økonomi
    ) {
        addInntekt(dato, økonomi)
    }

    private fun addInntekt(dato: LocalDate, økonomi: Økonomi) {
        økonomier.putIfAbsent(dato, mutableListOf(økonomi))?.add(økonomi)
    }

}
