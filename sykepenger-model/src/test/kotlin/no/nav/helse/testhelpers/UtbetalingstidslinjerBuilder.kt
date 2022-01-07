package no.nav.helse.testhelpers

import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Tar høyde for helg i dag-generering.
 * Eks: 12.NAV fra en mandag gir 10 sykedager i 2 hele arbeidsuker + 2 sykHelg-dager
 * */
internal fun tidslinjeOf(
    vararg utbetalingsdager: Utbetalingsdager,
    startDato: LocalDate = LocalDate.of(2018, 1, 1),
    skjæringstidspunkter: List<LocalDate> = listOf(startDato)
) = Utbetalingstidslinje().apply {
    val skjæringstidspunkt = { dato: LocalDate -> skjæringstidspunkter.filter { dato >= it }.maxOrNull() ?: dato }
    utbetalingsdager.fold(startDato) { startDato, (antallDagerFun, utbetalingsdag, helgedag, dekningsgrunnlag, grad, arbeidsgiverbeløp) ->
        var dato = startDato
        val antallDager = antallDagerFun(startDato)
        repeat(antallDager) {
            val økonomi = Økonomi.sykdomsgrad(grad.prosent)
                .inntekt(dekningsgrunnlag.daglig, skjæringstidspunkt = skjæringstidspunkt(dato))
                .arbeidsgiverRefusjon(arbeidsgiverbeløp.daglig)
            if (helgedag != null && dato.erHelg()) this.helgedag(dato, økonomi)
            else this.utbetalingsdag(dato, økonomi)
            dato = dato.plusDays(1)
        }
        dato
    }
}

internal val Int.ukedager get() = Ukedager(this)
internal operator fun LocalDate.plus(other: Ukedager) = other + this
internal class Ukedager(private val antallUkedager: Int) {
    private companion object {
        // tabellen er en sammenslått tabell på 5 kolonner og 7 rader (én for hver ukedag) som angir hvor mange
        // dager man skal addere med gitt ukedagen til datoen og hvor mange ukedager man skal addere
        // feks lørdag + 1 ukedag => 2 fordi man skal først hoppe over søndag og deretter ukedagen (mandag).
        // Et koordinat (x, y) i en 2D-tabell med w kolonner kan omgjøres til et punkt z i en 1D-tabell ved formelen z = f(x, y, w) = wx + y
        // https://support.claris.com/s/article/Calculating-a-Finish-Date-Given-a-Starting-Date-and-the-Number-of-Work-Days-1503692916564
        private const val table = "01234012360125601456034562345612345"
        private fun String.tilleggsdager(row: DayOfWeek, col: Int) = this[(row.value - 1) * 5 + col % 5].toString().toInt()
    }
    private fun dager(dato: LocalDate) =
        antallUkedager / 5 * 7 + table.tilleggsdager(dato.dayOfWeek, antallUkedager)
    operator fun plus(other: LocalDate): LocalDate = other.plusDays(dager(other).toLong())
}

internal val Int.AP get() = this.AP(1200)
internal fun Int.AP(dekningsgrunnlag: Int) = Utbetalingsdager(
    antallDager = { this },
    addDagFun = Utbetalingstidslinje::addArbeidsgiverperiodedag,
    dekningsgrunnlag = dekningsgrunnlag
)

internal val Int.NAV get() = this.NAV(1200)
internal fun Int.NAV(dekningsgrunnlag: Number, grad: Number = 100.0, refusjonsbeløp: Number = dekningsgrunnlag) = NAV({ this }, dekningsgrunnlag, grad, refusjonsbeløp)

private fun NAV(antallDager: (LocalDate) -> Int, dekningsgrunnlag: Number, grad: Number = 100.0, refusjonsbeløp: Number = dekningsgrunnlag) = Utbetalingsdager(
    antallDager = antallDager,
    addDagFun = Utbetalingstidslinje::addNAVdag,
    addHelgFun = Utbetalingstidslinje::addHelg,
    dekningsgrunnlag = dekningsgrunnlag,
    grad = grad,
    arbeidsgiverbeløp = refusjonsbeløp
)

internal val Int.HELG get() = this.HELG(1200)
internal fun Int.HELG(dekningsgrunnlag: Int, grad: Number = 100.0) = Utbetalingsdager(
    antallDager = { this },
    addDagFun = Utbetalingstidslinje::addHelg,
    dekningsgrunnlag = dekningsgrunnlag,
    grad = grad
)

internal val Int.NAVDAGER get() = this.NAVDAGER(1200)
internal fun Int.NAVDAGER(dekningsgrunnlag: Number, grad: Number = 100.0, refusjonsbeløp: Number = dekningsgrunnlag) =
    NAV({ ChronoUnit.DAYS.between(it, it.plus((this - 1).ukedager).plusDays(1)).toInt() }, dekningsgrunnlag, grad, refusjonsbeløp)

internal val Int.ARB get() = this.ARB(1200)
internal fun Int.ARB(dekningsgrunnlag: Int) = Utbetalingsdager(
    antallDager = { this },
    addDagFun = Utbetalingstidslinje::addArbeidsdag,
    dekningsgrunnlag = dekningsgrunnlag
)

internal val Int.FRI get() = this.FRI(1200)
internal fun Int.FRI(dekningsgrunnlag: Int) = Utbetalingsdager(
    antallDager = { this },
    addDagFun = Utbetalingstidslinje::addFridag,
    addHelgFun = Utbetalingstidslinje::addFridag,
    dekningsgrunnlag = dekningsgrunnlag
)

internal val Int.FOR get() = this.FOR(1200)
internal fun Int.FOR(dekningsgrunnlag: Int) = Utbetalingsdager(
    antallDager = { this },
    addDagFun = Utbetalingstidslinje::addForeldetDag,
    dekningsgrunnlag = dekningsgrunnlag
)

internal val Int.AVV get() = this.AVV(1200)
internal fun Int.AVV(dekningsgrunnlag: Int, grad: Number = 0) = Utbetalingsdager(
    antallDager = { this },
    addDagFun = Utbetalingstidslinje::addAvvistDag,
    dekningsgrunnlag = dekningsgrunnlag,
    grad = grad
)

internal val Int.UTELATE
    get() = Utbetalingsdager(
        antallDager = { this },
        addDagFun = { _, _ -> },
        dekningsgrunnlag = 0
    )

private fun Utbetalingstidslinje.addAvvistDag(dato: LocalDate, økonomi: Økonomi) =
    this.addAvvistDag(dato, økonomi, listOf(Begrunnelse.SykepengedagerOppbrukt))

internal data class Utbetalingsdager(
    val antallDager: (LocalDate) -> Int,
    val addDagFun: Utbetalingstidslinje.(LocalDate, Økonomi) -> Unit,
    val addHelgFun: (Utbetalingstidslinje.(LocalDate, Økonomi) -> Unit)? = null,
    val dekningsgrunnlag: Number = 1200,
    val grad: Number = 0.0,
    val arbeidsgiverbeløp: Number = dekningsgrunnlag
) {
    internal fun copyWith(beløp: Number? = null, grad: Number? = null, arbeidsgiverbeløp: Number? = null) = Utbetalingsdager(
        antallDager = this.antallDager,
        addDagFun = this.addDagFun,
        addHelgFun = this.addHelgFun,
        dekningsgrunnlag = beløp.takeIf { it != null } ?: this.dekningsgrunnlag,
        arbeidsgiverbeløp = arbeidsgiverbeløp.takeIf { it != null } ?: beløp ?: this.dekningsgrunnlag,
        grad = grad.takeIf { it != null } ?: this.grad
    )
}
