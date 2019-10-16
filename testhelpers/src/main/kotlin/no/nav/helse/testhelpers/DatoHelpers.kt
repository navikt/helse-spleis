package no.nav.helse.testhelpers

import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import java.time.LocalDate
import java.time.Month

val Int.juli get() = LocalDate.of(2019, Month.JULY, this)

val Int.mandag get() = 1.juli.plusWeeks(this.toLong() - 1)
val Int.tirsdag get() = this.mandag.plusDays(1)
val Int.onsdag get() = this.mandag.plusDays(2)
val Int.torsdag get() = this.mandag.plusDays(3)
val Int.fredag get() = this.mandag.plusDays(4)
val Int.lørdag get() = this.mandag.plusDays(5)
val Int.søndag get() = this.mandag.plusDays(6)

operator fun Sykdomstidslinje.get(index: LocalDate) = flatten().firstOrNull { it.startdato() == index }
