package no.nav.helse.fixtures

import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.time.LocalDate

internal fun tidslinjeOf(
    vararg dagPairs: Pair<Int, Utbetalingstidslinje.(Double, LocalDate) -> Unit>
) = Utbetalingstidslinje().apply {
    dagPairs.fold(LocalDate.of(2018, 1, 1)){ startDato, (antallDager, utbetalingsdag) ->
        (0 until antallDager).forEach {
            this.utbetalingsdag(1200.0, startDato.plusDays(it.toLong()))
        }
        startDato.plusDays(antallDager.toLong())
    }
}

internal val Int.AP get() = Pair(this, Utbetalingstidslinje::addArbeidsgiverperiodedag)
internal val Int.NAV get() = Pair(this, Utbetalingstidslinje::addNAVdag)
internal val Int.ARB get() = Pair(this, Utbetalingstidslinje::addArbeidsdag)
internal val Int.FRI get() = Pair(this, Utbetalingstidslinje::addFridag)
internal val Int.HELG get() = Pair(this, Utbetalingstidslinje::addHelg)
internal val Int.UTELATE get() = Pair(this, Utbetalingstidslinje::utelate)

private fun Utbetalingstidslinje.utelate(inntekt: Double, dato: LocalDate) {}
