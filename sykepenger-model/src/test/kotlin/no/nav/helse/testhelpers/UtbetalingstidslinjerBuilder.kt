package no.nav.helse.testhelpers

import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.time.LocalDate

internal fun tidslinjeOf(
    vararg dagTriples: Triple<Int, Utbetalingstidslinje.(Double, LocalDate, Double) -> Unit, Double>,
    startDato: LocalDate = LocalDate.of(2018, 1, 1)
) = Utbetalingstidslinje().apply {
    dagTriples.fold(startDato){ startDato, (antallDager, utbetalingsdag, inntekt) ->
        (0 until antallDager).forEach {
            this.utbetalingsdag(inntekt, startDato.plusDays(it.toLong()), 100.00)
        }
        startDato.plusDays(antallDager.toLong())
    }
}

internal val Int.AP get() = this.AP(1200.00)
internal fun Int.AP(inntekt: Double) = Triple(this, Utbetalingstidslinje::addArbeidsgiverperiodedag, inntekt)
internal val Int.NAV get() = this.NAV(1200.00)
internal fun Int.NAV(inntekt: Double) = Triple(this, Utbetalingstidslinje::addNAVdag, inntekt)
internal val Int.ARB get() = this.ARB(1200.00)
internal fun Int.ARB(inntekt: Double) = Triple(this, Utbetalingstidslinje::addArbeidsdag, inntekt)
internal val Int.FRI get() = this.FRI(1200.00)
internal fun Int.FRI(inntekt: Double) = Triple(this, Utbetalingstidslinje::addFridag, inntekt)
internal val Int.HELG get() = this.HELG(1200.00)
internal fun Int.HELG(inntekt: Double) = Triple(this, Utbetalingstidslinje::addHelg, inntekt)
internal val Int.UTELATE get() = Triple(this, Utbetalingstidslinje::utelate, 0.00)

private fun Utbetalingstidslinje.utelate(inntekt: Double, dato: LocalDate, grad: Double) {}
