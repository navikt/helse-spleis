package no.nav.helse.testhelpers

import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

/**
 * Tar høyde for helg i dag-generering.
 * Eks: 12.NAVv2 fra en mandag gir 10 sykedager i 2 hele arbeidsuker + 2 sykHelg-dager
 * */
internal fun tidslinjeOf(
    vararg utbetalingsdager: Utbetalingsdager,
    startDato: LocalDate = LocalDate.of(2018, 1, 1),
    skjæringstidspunkter: List<LocalDate> = listOf(startDato)
) = Utbetalingstidslinje().apply {
    val skjæringstidspunkt = { dato: LocalDate -> skjæringstidspunkter.filter { dato >= it }.maxOrNull() ?: dato }
    utbetalingsdager.fold(startDato) { startDato, (antallDager, utbetalingsdag, helgedag, dekningsgrunnlag, grad) ->
        var dato = startDato
        repeat(antallDager) {
            if (helgedag != null && dato.erHelg()) {
                this.helgedag(dato, Økonomi.sykdomsgrad(grad.prosent).inntekt(dekningsgrunnlag.daglig, skjæringstidspunkt = skjæringstidspunkt(dato)))
            } else {
                this.utbetalingsdag(dato, Økonomi.sykdomsgrad(grad.prosent).inntekt(dekningsgrunnlag.daglig, skjæringstidspunkt = skjæringstidspunkt(dato)))
            }
            dato = dato.plusDays(1)
        }
        dato
    }
}

internal val Int.AP get() = this.AP(1200)
internal fun Int.AP(dekningsgrunnlag: Int) = Utbetalingsdager(
    antallDager = this,
    addDagFun = Utbetalingstidslinje::addArbeidsgiverperiodedag,
    dekningsgrunnlag = dekningsgrunnlag
)

internal val Int.NAV get() = this.NAV(1200)
internal fun Int.NAV(dekningsgrunnlag: Number, grad: Number = 100.0) = Utbetalingsdager(
    antallDager = this,
    addDagFun = Utbetalingstidslinje::addNAVdag,
    dekningsgrunnlag = dekningsgrunnlag,
    grad = grad
)

internal val Int.ARB get() = this.ARB(1200)
internal fun Int.ARB(dekningsgrunnlag: Int) = Utbetalingsdager(
    antallDager = this,
    addDagFun = Utbetalingstidslinje::addArbeidsdag,
    dekningsgrunnlag = dekningsgrunnlag
)

internal val Int.FRI get() = this.FRI(1200)
internal fun Int.FRI(dekningsgrunnlag: Int) = Utbetalingsdager(
    antallDager = this,
    addDagFun = Utbetalingstidslinje::addFridag,
    addHelgFun = Utbetalingstidslinje::addFridag,
    dekningsgrunnlag = dekningsgrunnlag
)

internal val Int.FOR get() = this.FOR(1200)
internal fun Int.FOR(dekningsgrunnlag: Int) = Utbetalingsdager(
    antallDager = this,
    addDagFun = Utbetalingstidslinje::addForeldetDag,
    dekningsgrunnlag = dekningsgrunnlag
)

internal val Int.AVV get() = this.AVV(1200)
internal fun Int.AVV(dekningsgrunnlag: Int, grad: Number = 0) = Utbetalingsdager(
    antallDager = this,
    addDagFun = Utbetalingstidslinje::addAvvistDag,
    dekningsgrunnlag = dekningsgrunnlag,
    grad = grad
)

internal val Int.HELG get() = this.HELG(1200)
internal fun Int.HELG(dekningsgrunnlag: Int, grad: Number = 100.0) = Utbetalingsdager(
    antallDager = this,
    addDagFun = Utbetalingstidslinje::addHelg,
    dekningsgrunnlag = dekningsgrunnlag,
    grad = grad
)

internal val Int.UTELATE
    get() = Utbetalingsdager(
        antallDager = this,
        addDagFun = { _, _ -> },
        dekningsgrunnlag = 0
    )

internal val Int.NAVv2 get() = this.NAVv2(1200)
internal fun Int.NAVv2(dekningsgrunnlag: Number, grad: Number = 100.0) = Utbetalingsdager(
    antallDager = this,
    addDagFun = Utbetalingstidslinje::addNAVdag,
    addHelgFun = Utbetalingstidslinje::addHelg,
    dekningsgrunnlag = dekningsgrunnlag,
    grad = grad
)

internal val Int.ARBv2 get() = this.ARBv2(1200)
internal fun Int.ARBv2(dekningsgrunnlag: Int) = Utbetalingsdager(
    antallDager = this,
    addDagFun = Utbetalingstidslinje::addArbeidsdag,
    addHelgFun = Utbetalingstidslinje::addFridag,
    dekningsgrunnlag = dekningsgrunnlag
)

internal val Int.FRIv2 get() = this.FRIv2(1200)
internal fun Int.FRIv2(dekningsgrunnlag: Int) = Utbetalingsdager(
    antallDager = this,
    addDagFun = Utbetalingstidslinje::addFridag,
    addHelgFun = Utbetalingstidslinje::addFridag,
    dekningsgrunnlag = dekningsgrunnlag
)

internal val Int.FORv2 get() = this.FORv2(1200)
internal fun Int.FORv2(dekningsgrunnlag: Int) = Utbetalingsdager(
    antallDager = this,
    addDagFun = Utbetalingstidslinje::addForeldetDag,
    addHelgFun = Utbetalingstidslinje::addForeldetDag,
    dekningsgrunnlag = dekningsgrunnlag
)

internal val Int.AVVv2 get() = this.AVVv2(1200)
internal fun Int.AVVv2(dekningsgrunnlag: Int, grad: Number = 0) = Utbetalingsdager(
    antallDager = this,
    addDagFun = Utbetalingstidslinje::addAvvistDag,
    addHelgFun = Utbetalingstidslinje::addAvvistDag,
    dekningsgrunnlag = dekningsgrunnlag,
    grad = grad
)

internal val Int.HELGv2 get() = this.HELGv2(1200)
internal fun Int.HELGv2(dekningsgrunnlag: Int, grad: Number = 100.0) = Utbetalingsdager(
    antallDager = this,
    addDagFun = Utbetalingstidslinje::addHelg,
    addHelgFun = Utbetalingstidslinje::addHelg,
    dekningsgrunnlag = dekningsgrunnlag,
    grad = grad
)

private fun Utbetalingstidslinje.addAvvistDag(dato: LocalDate, økonomi: Økonomi) =
    this.addAvvistDag(dato, økonomi, listOf(Begrunnelse.SykepengedagerOppbrukt))

internal data class Utbetalingsdager(
    val antallDager: Int,
    val addDagFun: Utbetalingstidslinje.(LocalDate, Økonomi) -> Unit,
    val addHelgFun: (Utbetalingstidslinje.(LocalDate, Økonomi) -> Unit)? = null,
    val dekningsgrunnlag: Number = 1200,
    val grad: Number = 0.0
) {
    internal fun copyWith(beløp: Number? = null, grad: Number? = null) = Utbetalingsdager(
        antallDager = this.antallDager,
        addDagFun = this.addDagFun,
        addHelgFun = this.addHelgFun,
        dekningsgrunnlag = beløp.takeIf { it != null } ?: this.dekningsgrunnlag,
        grad = grad.takeIf { it != null } ?: this.grad
    )
}
