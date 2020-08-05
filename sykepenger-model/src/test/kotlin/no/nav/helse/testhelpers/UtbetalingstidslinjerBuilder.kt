package no.nav.helse.testhelpers

import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

internal fun tidslinjeOf(
    vararg utbetalingsdager: Utbetalingsdager,
    startDato: LocalDate = LocalDate.of(2018, 1, 1)
) = Utbetalingstidslinje().apply {
    utbetalingsdager.fold(startDato){ startDato, (antallDager, utbetalingsdag, dekningsgrunnlag, grad) ->
        (0 until antallDager).forEach {
            this.utbetalingsdag(startDato.plusDays(it.toLong()), Økonomi.sykdomsgrad(grad.prosent).inntekt(dekningsgrunnlag.daglig))
        }
        startDato.plusDays(antallDager.toLong())
    }
}

internal val Int.AP get() = this.AP(1200)
internal fun Int.AP(dekningsgrunnlag: Int) = Utbetalingsdager(this, Utbetalingstidslinje::addArbeidsgiverperiodedag, dekningsgrunnlag)
internal val Int.NAV get() = this.NAV(1200)
internal fun Int.NAV(dekningsgrunnlag: Number, grad: Number = 100.0) = Utbetalingsdager(this, Utbetalingstidslinje::addNAVdag, dekningsgrunnlag, grad)
internal val Int.ARB get() = this.ARB(1200)
internal fun Int.ARB(dekningsgrunnlag: Int) = Utbetalingsdager(this, Utbetalingstidslinje::addArbeidsdag, dekningsgrunnlag)
internal val Int.FRI get() = this.FRI(1200)
internal fun Int.FRI(dekningsgrunnlag: Int) = Utbetalingsdager(this, Utbetalingstidslinje::addFridag, dekningsgrunnlag)
internal val Int.FOR get() = this.FOR(1200)
internal fun Int.FOR(dekningsgrunnlag: Int) = Utbetalingsdager(this, Utbetalingstidslinje::addForeldetDag, dekningsgrunnlag)
internal val Int.AVV get() = this.AVV(1200)
internal fun Int.AVV(dekningsgrunnlag: Int, grad: Number = 0) = Utbetalingsdager(this, Utbetalingstidslinje::addAvvistDag, dekningsgrunnlag, grad)
internal val Int.HELG get() = this.HELG(1200)
internal fun Int.HELG(dekningsgrunnlag: Int, grad: Number = 100.0) = Utbetalingsdager(this, Utbetalingstidslinje::addHelg, dekningsgrunnlag, grad)
internal val Int.UTELATE get() = Utbetalingsdager(this, { _, _ -> }, 0)

private fun Utbetalingstidslinje.addAvvistDag(dato: LocalDate, økonomi: Økonomi) =
    this.addAvvistDag(dato, økonomi, Begrunnelse.MinimumSykdomsgrad)
internal data class Utbetalingsdager(
    val antallDager: Int,
    val addDagFun: Utbetalingstidslinje.(LocalDate, Økonomi) -> Unit,
    val dekningsgrunnlag: Number = 1200,
    val grad: Number = 0.0
)
