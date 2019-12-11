package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Testhendelse
import no.nav.helse.testhelpers.Uke
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class HendelserTest {

    @Test
    fun `kan hente ut hendelser av en tidslinje`() {
        val hendelse = Testhendelse()

        val tidslinje = ConcreteSykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).fredag, hendelse)
        val hendelser = tidslinje.hendelser()

        assertEquals(1, hendelser.size)
        assertEquals(hendelse, hendelser.first())
    }

    @Test
    fun `får med hendelser fra erstattede dager`() {
        val hendelse = Testhendelse()
        val hendelse2 = Testhendelse()

        val tidslinje = ConcreteSykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).fredag, hendelse) + ConcreteSykdomstidslinje.ikkeSykedager(Uke(1).mandag, Uke(1).fredag, hendelse2)
        val hendelser = tidslinje.hendelser()

        assertEquals(2, hendelser.size)
        assertTrue(hendelser.containsAll(listOf(hendelse, hendelse2)))
    }
}
