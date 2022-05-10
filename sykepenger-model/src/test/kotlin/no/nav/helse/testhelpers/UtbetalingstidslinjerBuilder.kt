package no.nav.helse.testhelpers

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import no.nav.helse.plus
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.ukedager
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi

/**
 * Tar høyde for helg i dag-generering.
 * Eks: 12.NAV fra en mandag gir 10 sykedager i 2 hele arbeidsuker + 2 sykHelg-dager
 * */
internal fun tidslinjeOf(
    vararg utbetalingsdager: Utbetalingsdager,
    startDato: LocalDate = LocalDate.of(2018, 1, 1),
    skjæringstidspunkter: List<LocalDate> = listOf(startDato)
) = Utbetalingstidslinje.Builder().apply {
    val skjæringstidspunkt = { dato: LocalDate -> skjæringstidspunkter.filter { dato >= it }.maxOrNull() ?: dato }
    utbetalingsdager.fold(startDato) { startDato, (antallDagerFun, utbetalingsdag, helgedag, dekningsgrunnlag, grad, arbeidsgiverbeløp) ->
        var dato = startDato
        val antallDager = antallDagerFun(startDato)
        repeat(antallDager) {
            val økonomi = Økonomi.sykdomsgrad(grad.prosent)
                .inntekt(dekningsgrunnlag, skjæringstidspunkt = skjæringstidspunkt(dato))
                .arbeidsgiverRefusjon(arbeidsgiverbeløp)
            if (helgedag != null && dato.erHelg()) this.helgedag(dato, økonomi)
            else this.utbetalingsdag(dato, økonomi)
            dato = dato.plusDays(1)
        }
        dato
    }
}.build()

internal val Int.AP get() = this.AP(1200)
internal fun Int.AP(dekningsgrunnlag: Int) = Utbetalingsdager(
    antallDager = { this },
    addDagFun = Utbetalingstidslinje.Builder::addArbeidsgiverperiodedag,
    dekningsgrunnlag = dekningsgrunnlag.daglig
)

internal val Int.NAV get() = this.NAV(1200)
internal fun Int.NAV(dekningsgrunnlag: Number, grad: Number = 100.0, refusjonsbeløp: Number = dekningsgrunnlag) = NAV({ this }, dekningsgrunnlag.daglig, grad, refusjonsbeløp.daglig)
internal fun Int.NAV(dekningsgrunnlag: Inntekt, grad: Number = 100.0, refusjonsbeløp: Inntekt = dekningsgrunnlag) = NAV({ this }, dekningsgrunnlag, grad, refusjonsbeløp)

private fun NAV(antallDager: (LocalDate) -> Int, dekningsgrunnlag: Inntekt, grad: Number = 100.0, refusjonsbeløp: Inntekt = dekningsgrunnlag) = Utbetalingsdager(
    antallDager = antallDager,
    addDagFun = Utbetalingstidslinje.Builder::addNAVdag,
    addHelgFun = Utbetalingstidslinje.Builder::addHelg,
    dekningsgrunnlag = dekningsgrunnlag,
    grad = grad,
    arbeidsgiverbeløp = refusjonsbeløp
)

internal val Int.HELG get() = this.HELG(1200)
internal fun Int.HELG(dekningsgrunnlag: Int, grad: Number = 100.0) = Utbetalingsdager(
    antallDager = { this },
    addDagFun = Utbetalingstidslinje.Builder::addHelg,
    dekningsgrunnlag = dekningsgrunnlag.daglig,
    grad = grad
)

internal val Int.NAVDAGER get() = this.NAVDAGER(1200)
internal fun Int.NAVDAGER(dekningsgrunnlag: Number, grad: Number = 100.0, refusjonsbeløp: Number = dekningsgrunnlag) =
    NAV({ ChronoUnit.DAYS.between(it, it.plus((this - 1).ukedager).plusDays(1)).toInt() }, dekningsgrunnlag.daglig, grad, refusjonsbeløp.daglig)

internal val Int.ARB get() = this.ARB(1200)
internal fun Int.ARB(dekningsgrunnlag: Int) = Utbetalingsdager(
    antallDager = { this },
    addDagFun = Utbetalingstidslinje.Builder::addArbeidsdag,
    dekningsgrunnlag = dekningsgrunnlag.daglig
)

internal val Int.FRI get() = this.FRI(1200)
internal fun Int.FRI(dekningsgrunnlag: Int) = Utbetalingsdager(
    antallDager = { this },
    addDagFun = Utbetalingstidslinje.Builder::addFridag,
    addHelgFun = Utbetalingstidslinje.Builder::addFridag,
    dekningsgrunnlag = dekningsgrunnlag.daglig
)

internal val Int.FOR get() = this.FOR(1200)
internal fun Int.FOR(dekningsgrunnlag: Int) = Utbetalingsdager(
    antallDager = { this },
    addDagFun = Utbetalingstidslinje.Builder::addForeldetDag,
    dekningsgrunnlag = dekningsgrunnlag.daglig
)

internal val Int.AVV get() = this.AVV(1200)
internal fun Int.AVV(dekningsgrunnlag: Int, grad: Number = 0) = Utbetalingsdager(
    antallDager = { this },
    addDagFun = Utbetalingstidslinje.Builder::addAvvistDag,
    dekningsgrunnlag = dekningsgrunnlag.daglig,
    grad = grad
)

internal val Int.UTELATE
    get() = Utbetalingsdager(
        antallDager = { this },
        addDagFun = { _, _ -> },
        dekningsgrunnlag = INGEN
    )

private fun Utbetalingstidslinje.Builder.addAvvistDag(dato: LocalDate, økonomi: Økonomi) =
    this.addAvvistDag(dato, økonomi, listOf(Begrunnelse.SykepengedagerOppbrukt))

internal data class Utbetalingsdager(
    val antallDager: (LocalDate) -> Int,
    val addDagFun: Utbetalingstidslinje.Builder.(LocalDate, Økonomi) -> Unit,
    val addHelgFun: (Utbetalingstidslinje.Builder.(LocalDate, Økonomi) -> Unit)? = null,
    val dekningsgrunnlag: Inntekt = 1200.daglig,
    val grad: Number = 0.0,
    val arbeidsgiverbeløp: Inntekt = dekningsgrunnlag
) {
    internal fun copyWith(beløp: Number? = null, grad: Number? = null, arbeidsgiverbeløp: Number? = null) = Utbetalingsdager(
        antallDager = this.antallDager,
        addDagFun = addDagFun,
        addHelgFun = addHelgFun,
        dekningsgrunnlag = beløp?.daglig ?: this.dekningsgrunnlag,
        arbeidsgiverbeløp = arbeidsgiverbeløp?.daglig ?: beløp?.daglig ?: this.dekningsgrunnlag,
        grad = grad ?: this.grad
    )
}
