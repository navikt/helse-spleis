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
            this.utbetalingsdag(startDato.plusDays(it.toLong()), Økonomi.sykdomsgrad(grad.prosent).dekningsgrunnlag(dagsats))
        }
        startDato.plusDays(antallDager.toLong())
    }
}

internal val Int.AP get() = this.AP(1200)
internal fun Int.AP(dagsats: Int) = Utbetalingsdager(this, Utbetalingstidslinje::addArbeidsgiverperiodedag, dagsats)
internal val Int.NAV get() = this.NAV(1200)
internal fun Int.NAV(dagsats: Int, grad: Number = 100.0) = Utbetalingsdager(this, Utbetalingstidslinje::addNAVdag, dagsats, grad)
internal val Int.ARB get() = this.ARB(1200)
internal fun Int.ARB(dagsats: Int) = Utbetalingsdager(this, Utbetalingstidslinje::addArbeidsdag, dagsats)
internal val Int.FRI get() = this.FRI(1200)
internal fun Int.FRI(dagsats: Int) = Utbetalingsdager(this, Utbetalingstidslinje::addFridag, dagsats)
internal val Int.FOR get() = this.FOR(1200)
internal fun Int.FOR(dagsats: Int) = Utbetalingsdager(this, Utbetalingstidslinje::addForeldetDag, dagsats)
internal val Int.AVV get() = this.AVV(1200)
internal fun Int.AVV(dagsats: Int) = Utbetalingsdager(this, Utbetalingstidslinje::addAvvistDag, dagsats)
internal val Int.HELG get() = this.HELG(1200)
internal fun Int.HELG(dagsats: Int, grad: Number = 100.0) = Utbetalingsdager(this, Utbetalingstidslinje::addHelg, dagsats, grad)
internal val Int.UTELATE get() = Utbetalingsdager(this, { _, _ -> }, 0)

private fun Utbetalingstidslinje.addAvvistDag(dato: LocalDate, økonomi: Økonomi) =
    this.addAvvistDag(dato, økonomi, Begrunnelse.MinimumSykdomsgrad)
internal data class Utbetalingsdager(
    val antallDager: Int,
    val addDagFun: Utbetalingstidslinje.(LocalDate, Økonomi) -> Unit,
    val dagsats: Int = 1200,
    val grad: Number = 0.0
)
