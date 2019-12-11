package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Testhendelse
import no.nav.helse.testhelpers.Uke
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class DateRangeTest {
    private val sendtSøknad = Testhendelse()

    @Test
    fun påfølgendeSykedager() {
        val sykedager = ConcreteSykdomstidslinje.sykedager(Uke(1).tirsdag, Uke(1).onsdag, sendtSøknad)

        assertEquals(Uke(1).tirsdag, sykedager.førsteDag())
        assertEquals(Uke(1).onsdag, sykedager.sisteDag())
    }

    @Test
    fun sluttForStartFeiler() {
        assertThrows<IllegalArgumentException> { ConcreteSykdomstidslinje.sykedager(Uke(1).onsdag, Uke(1).tirsdag, sendtSøknad) }
    }

    @Test
    fun sammeDagErEnDag() {
        val sykedager = ConcreteSykdomstidslinje.sykedager(Uke(1).tirsdag, Uke(1).tirsdag, sendtSøknad)

        assertEquals(Uke(1).tirsdag, sykedager.førsteDag())
        assertEquals(Uke(1).tirsdag, sykedager.sisteDag())
    }

    @Test
    fun mandagTilMandagIkkeSyk() {
        val interval = ConcreteSykdomstidslinje.ikkeSykedager(Uke(1).mandag, Uke(2).mandag, sendtSøknad)

        val dager = interval.flatten()

        assertEquals(8, dager.size)
    }

    @Test
    fun mandagTilMandagSyk() {
        val interval = ConcreteSykdomstidslinje.sykedager(Uke(1).mandag, Uke(2).mandag, sendtSøknad)

        val dager = interval.flatten()
        assertEquals(8, dager.size)
    }

    @Test
    fun mandagTilMandagVirkedagerSyk() {
        val interval = ConcreteSykdomstidslinje.sykedager(Uke(1).mandag, Uke(2).mandag, sendtSøknad)

        val dager = interval.flatten()
        assertEquals(8, dager.size)
    }
}
