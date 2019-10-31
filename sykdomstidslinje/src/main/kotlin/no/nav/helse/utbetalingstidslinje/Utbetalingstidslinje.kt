package no.nav.helse.utbetalingstidslinje

import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.Syketilfelle
import no.nav.helse.sykdomstidslinje.dag.Dag
import no.nav.helse.sykdomstidslinje.dag.Feriedag
import no.nav.helse.sykdomstidslinje.dag.Sykedag
import java.math.BigDecimal

 class Utbetalingstidslinje(val dagsats: BigDecimal, val utbetalingsdager: List<Utbetalingsdag>)

fun Syketilfelle.tilUtbetalingstidslinjer(): List<Utbetalingstidslinje> {
    val utbetalingsdager = arbeidsgiverperiode.tilUtbetalingsdager(true) +
            (dagerEtterArbeidsgiverperiode?.tilUtbetalingsdager(false) ?: emptyList())

    return utbetalingsdager.filter { it.dag is Sykedag || it.dag.erHelg() || it.dag is Feriedag }
        .fold(mutableListOf(), splitUtbetalingstilfeller())
        .map { utbetalingsdager -> Utbetalingstidslinje(
            BigDecimal.TEN,
            utbetalingsdager.filterNot { it.dag.erHelg() || it.dag is Feriedag }) }
}

private fun Sykdomstidslinje.tilUtbetalingsdager(arbeidsgiverperiode: Boolean) =
    this.flatten().map { Utbetalingsdag(it, arbeidsgiverperiode) }

private fun splitUtbetalingstilfeller(): (MutableList<MutableList<Utbetalingsdag>>, Utbetalingsdag) -> MutableList<MutableList<Utbetalingsdag>> {
    return { utbetalingstilfeller, utbetalingsdag ->
        when {
            utbetalingstilfeller.isEmpty() -> utbetalingstilfeller.add(mutableListOf(utbetalingsdag))
            utbetalingstilfeller.last().last().erPåfølgendeDag(utbetalingsdag) -> utbetalingstilfeller.last().add(
                utbetalingsdag
            )
            utbetalingsdag.dag is Sykedag -> utbetalingstilfeller.add(mutableListOf(utbetalingsdag))
        }
        utbetalingstilfeller
    }
}

data class Utbetalingsdag(val dag: Dag, val arbeidsgiverperiode: Boolean) {
    internal fun erPåfølgendeDag(other: Utbetalingsdag) = other.dag.dagen.minusDays(1) == dag.dagen
}