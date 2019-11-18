package no.nav.helse.testhelpers

import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import java.time.LocalDate

internal class Uke(ukenr: Long) {
    val mandag = LocalDate.of(2018, 1, 1)
        .plusWeeks(ukenr - 1L)
    val tirsdag get() = mandag.plusDays(1)
    val onsdag get() = mandag.plusDays(2)
    val torsdag get() = mandag.plusDays(3)
    val fredag get() = mandag.plusDays(4)
    val lørdag get() = mandag.plusDays(5)
    val søndag get() = mandag.plusDays(6)
}

internal operator fun Sykdomstidslinje.get(index: LocalDate) = flatten().firstOrNull { it.startdato() == index }
