package no.nav.helse

import java.time.DayOfWeek
import java.time.LocalDate

fun Int.januar(year: Int): LocalDate = LocalDate.of(year, 1, this)
fun Int.februar(year: Int): LocalDate = LocalDate.of(year, 2, this)
fun Int.mars(year: Int): LocalDate = LocalDate.of(year, 3, this)
fun Int.april(year: Int): LocalDate = LocalDate.of(year, 4, this)
fun Int.mai(year: Int): LocalDate = LocalDate.of(year, 5, this)
fun Int.juni(year: Int): LocalDate = LocalDate.of(year, 6, this)
fun Int.juli(year: Int): LocalDate = LocalDate.of(year, 7, this)
fun Int.august(year: Int): LocalDate = LocalDate.of(year, 8, this)
fun Int.september(year: Int): LocalDate = LocalDate.of(year, 9, this)
fun Int.oktober(year: Int): LocalDate = LocalDate.of(year, 10, this)
fun Int.november(year: Int): LocalDate = LocalDate.of(year, 11, this)
fun Int.desember(year: Int): LocalDate = LocalDate.of(year, 12, this)

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

fun LocalDate.nesteArbeidsdag(): LocalDate = this + 0.ukedager
