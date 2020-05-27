package no.nav.helse.testhelpers

import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.prosent
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

internal fun tidslinjeOf(
    vararg utbetalingsdager: Utbetalingsdager,
    startDato: LocalDate = LocalDate.of(2018, 1, 1)
) = Utbetalingstidslinje().apply {
    utbetalingsdager.fold(startDato){ startDato, (antallDager, utbetalingsdag, dagsats, grad) ->
        (0 until antallDager).forEach {
            this.utbetalingsdag(dagsats, startDato.plusDays(it.toLong()), grad)
        }
        startDato.plusDays(antallDager.toLong())
    }
}

internal val Int.AP get() = this.AP(1200)
internal fun Int.AP(dagsats: Int) = Utbetalingsdager(this, Utbetalingstidslinje::addArbeidsgiverperiodedag, dagsats)
internal val Int.NAV get() = this.NAV(1200)
internal fun Int.NAV(dagsats: Int, grad: Double = 100.0) = Utbetalingsdager(this, Utbetalingstidslinje::addNAVdag, dagsats, grad)
internal val Int.ARB get() = this.ARB(1200)
internal fun Int.ARB(dagsats: Int) = Utbetalingsdager(this, Utbetalingstidslinje::addArbeidsdag, dagsats)
internal val Int.FRI get() = this.FRI(1200)
internal fun Int.FRI(dagsats: Int) = Utbetalingsdager(this, Utbetalingstidslinje::addFridag, dagsats)
internal val Int.FOR get() = this.FOR(1200)
internal fun Int.FOR(dagsats: Int) = Utbetalingsdager(this, Utbetalingstidslinje::addForeldetDag, dagsats)
internal val Int.AVV get() = this.AVV(1200)
internal fun Int.AVV(dagsats: Int) = Utbetalingsdager(this, Utbetalingstidslinje::addAvvistDag, dagsats)
internal val Int.HELG get() = this.HELG(1200)
internal fun Int.HELG(dagsats: Int, grad: Double = 100.0) = Utbetalingsdager(this, Utbetalingstidslinje::addHelg, dagsats, grad)
internal val Int.UTELATE get() = Utbetalingsdager(this, { _, _, _ -> }, 0)

private fun Utbetalingstidslinje.addForeldetDag(dagsats: Int, dato: LocalDate, grad: Double) =
    this.addForeldetDag(dato, Økonomi.ikkeBetalt().dagsats(dagsats))
private fun Utbetalingstidslinje.addAvvistDag(dagsats: Int, dato: LocalDate, grad: Double) =
    this.addAvvistDag(dato, Økonomi.sykdomsgrad(grad.prosent).dagsats(dagsats), Begrunnelse.MinimumSykdomsgrad)
private fun Utbetalingstidslinje.addNAVdag(dagsats: Int, dato: LocalDate, grad: Double) =
    this.addNAVdag(dato, Økonomi.sykdomsgrad(grad.prosent).dagsats(dagsats))
private fun Utbetalingstidslinje.addHelg(dagsats: Int, dato: LocalDate, grad: Double) =
    this.addHelg(dato, Økonomi.sykdomsgrad(grad.prosent).dagsats(0))
private fun Utbetalingstidslinje.addArbeidsgiverperiodedag(dagsats: Int, dato: LocalDate, grad: Double) =
    this.addArbeidsgiverperiodedag(dato, Økonomi.ikkeBetalt().dagsats(dagsats))
private fun Utbetalingstidslinje.addArbeidsdag(dagsats: Int, dato: LocalDate, grad: Double) =
    this.addArbeidsdag(dato, Økonomi.ikkeBetalt().dagsats(dagsats))
private fun Utbetalingstidslinje.addFridag(dagsats: Int, dato: LocalDate, grad: Double) =
    this.addFridag(dato, Økonomi.ikkeBetalt().dagsats(dagsats))
internal data class Utbetalingsdager(
    val antallDager: Int,
    val addDagFun: Utbetalingstidslinje.(Int, LocalDate, Double) -> Unit,
    val dagsats: Int = 1200,
    val grad: Double = 0.0
)
