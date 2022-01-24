package no.nav.helse

import java.time.LocalDate

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
