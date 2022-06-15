package no.nav.helse

import java.time.LocalDate
import java.time.YearMonth

// 2018 har blitt valgt fordi det starter på en mandag og er ikke et skuddår
private const val startår = 2018
private val mandagsfrø = LocalDate.of(startår, 1, 1)
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