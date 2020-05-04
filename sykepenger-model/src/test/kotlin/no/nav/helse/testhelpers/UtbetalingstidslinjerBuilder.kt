package no.nav.helse.testhelpers

import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.time.LocalDate

internal fun tidslinjeOf(
    vararg utbetalingsdager: Utbetalingsdager,
    startDato: LocalDate = LocalDate.of(2018, 1, 1)
) = Utbetalingstidslinje().apply {
    utbetalingsdager.fold(startDato){ startDato, (antallDager, utbetalingsdag, inntekt, grad) ->
        (0 until antallDager).forEach {
            this.utbetalingsdag(inntekt, startDato.plusDays(it.toLong()), grad)
        }
        startDato.plusDays(antallDager.toLong())
    }
}

internal val Int.AP get() = this.AP(1200.00)
internal fun Int.AP(inntekt: Double) = Utbetalingsdager(this, Utbetalingstidslinje::addArbeidsgiverperiodedag, inntekt)
internal val Int.NAV get() = this.NAV(1200.00)
internal fun Int.NAV(inntekt: Double, grad: Double = 100.0) = Utbetalingsdager(this, Utbetalingstidslinje::addNAVdag, inntekt, grad)
internal val Int.ARB get() = this.ARB(1200.00)
internal fun Int.ARB(inntekt: Double) = Utbetalingsdager(this, Utbetalingstidslinje::addArbeidsdag, inntekt)
internal val Int.FRI get() = this.FRI()
internal fun Int.FRI() = Utbetalingsdager(this, { _, dagen, grad -> addFridag(dagen, grad) })
internal val Int.FOR get() = this.FOR(1200.00)
internal fun Int.FOR(inntekt: Double) = Utbetalingsdager(this, Utbetalingstidslinje::addForeldetDag, inntekt)
internal val Int.AVV get() = this.AVV(1200.00)
internal fun Int.AVV(inntekt: Double) = Utbetalingsdager(this, Utbetalingstidslinje::addAvvistDag, inntekt)
internal val Int.HELG get() = this.HELG(1200.00)
internal fun Int.HELG(inntekt: Double, grad: Double = 100.0) = Utbetalingsdager(this, Utbetalingstidslinje::addHelg, inntekt, grad)
internal val Int.UTELATE get() = Utbetalingsdager(this, { _, _, _ -> }, 0.00)

private fun Utbetalingstidslinje.addForeldetDag(inntekt: Double, dato: LocalDate, grad: Double) =
    this.addForeldetDag(dato)
private fun Utbetalingstidslinje.addAvvistDag(inntekt: Double, dato: LocalDate, grad: Double) =
    this.addAvvistDag(inntekt, dato, grad, Begrunnelse.MinimumSykdomsgrad)
internal data class Utbetalingsdager(
    val antallDager: Int,
    val addDagFun: Utbetalingstidslinje.(Double, LocalDate, Double) -> Unit,
    val inntekt: Double = 1200.0,
    val grad: Double = 0.0
)
