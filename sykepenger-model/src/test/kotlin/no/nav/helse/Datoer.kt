package no.nav.helse

import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import java.time.YearMonth
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til

// 2018 har blitt valgt fordi det starter på en mandag og er ikke et skuddår
private const val startår = 2018
private val mandagsfrø = LocalDate.of(startår, 1, 1) // fredet variabel

val januar: Periode = 1.januar til 31.januar
val februar: Periode = 1.februar til 28.februar
val mars: Periode = 1.mars til 31.mars
val april: Periode = 1.april til 30.april
val mai: Periode = 1.mai til 31.mai
val juni: Periode = 1.juni til 30.juni
val juli: Periode = 1.juli til 31.juli
val august: Periode = 1.august til 31.august
val september: Periode = 1.september til 30.september
val oktober: Periode = 1.oktober til 31.oktober
val november: Periode = 1.november til 30.november
val desember: Periode = 1.desember til 31.desember

val Int.mandag get() = mandagsfrø.plusWeeks(this.toLong() - 1)
val Int.tirsdag get() = this.mandag.plusDays(1)
val Int.onsdag get() = this.mandag.plusDays(2)
val Int.torsdag get() = this.mandag.plusDays(3)
val Int.fredag get() = this.mandag.plusDays(4)
val Int.lørdag get() = this.mandag.plusDays(5)
val Int.søndag get() = this.mandag.plusDays(6)

val Int.januar get() = this.januar(startår)
val Int.februar get() = this.februar(startår)
val Int.mars get() = this.mars(startår)
val Int.april get() = this.april(startår)
val Int.mai get() = this.mai(startår)
val Int.juni get() = this.juni(startår)
val Int.juli get() = this.juli(startår)
val Int.august get() = this.august(startår)
val Int.september get() = this.september(startår)
val Int.oktober get() = this.oktober(startår)
val Int.november get() = this.november(startår)
val Int.desember get() = this.desember(startår)

fun januar(år: Int) = YearMonth.of(år, 1)
fun februar(år: Int) = YearMonth.of(år, 2)
fun mars(år: Int) = YearMonth.of(år, 3)
fun april(år: Int) = YearMonth.of(år, 4)
fun mai(år: Int) = YearMonth.of(år, 5)
fun juni(år: Int) = YearMonth.of(år, 6)
fun juli(år: Int) = YearMonth.of(år, 7)
fun august(år: Int) = YearMonth.of(år, 8)
fun september(år: Int) = YearMonth.of(år, 9)
fun oktober(år: Int) = YearMonth.of(år, 10)
fun november(år: Int) = YearMonth.of(år, 11)
fun desember(år: Int) = YearMonth.of(år, 12)

infix fun LocalDate.i(år: Int) = withYear(år)
infix fun Periode.i(år: Int) = (start i år) til (endInclusive i år)

fun mandag(dato: LocalDate) = MONDAY.checkDayOfWeek(dato)
fun tirsdag(dato: LocalDate) = TUESDAY.checkDayOfWeek(dato)
fun onsdag(dato: LocalDate) = WEDNESDAY.checkDayOfWeek(dato)
fun torsdag(dato: LocalDate) = THURSDAY.checkDayOfWeek(dato)
fun fredag(dato: LocalDate) = FRIDAY.checkDayOfWeek(dato)
fun lørdag(dato: LocalDate) = SATURDAY.checkDayOfWeek(dato)
fun søndag(dato: LocalDate) = SUNDAY.checkDayOfWeek(dato)
private fun DayOfWeek.checkDayOfWeek(dato: LocalDate) = dato.also {
    check(this == dato.dayOfWeek) { "Forventet at $dato skulle være $this, men var ${dato.dayOfWeek}" }
}

sealed interface Ukedag {
    // mandag den 1.januar
    infix fun den(dato: LocalDate) = dato.also {
        check(
            it.dayOfWeek == when (this) {
                mandag -> MONDAY
                tirsdag -> TUESDAY
                onsdag -> WEDNESDAY
                torsdag -> THURSDAY
                fredag -> FRIDAY
                lørdag -> SATURDAY
                søndag -> SUNDAY
            }
        ) { "Forventet at $dato skulle være $this, men var ${dato.dayOfWeek}" }
    }
}

object mandag : Ukedag
object tirsdag : Ukedag
object onsdag : Ukedag
object torsdag : Ukedag
object fredag : Ukedag
object lørdag : Ukedag
object søndag : Ukedag

// 1.januar til tirsdag den 2.januar
infix fun LocalDate.til(ukedag: Ukedag) = this to ukedag
infix fun Pair<LocalDate, Ukedag>.den(other: LocalDate) = this.first til this.second.den(other)
