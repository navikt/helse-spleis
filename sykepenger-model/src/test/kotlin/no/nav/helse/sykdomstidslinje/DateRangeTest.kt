package no.nav.helse.sykdomstidslinje

import no.nav.helse.sykdomstidslinje.dag.Dag
import no.nav.helse.testhelpers.Uke
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class DateRangeTest {
    @Test
    fun påfølgendeSykedager() {
        val sykedager = ConcreteSykdomstidslinje.sykedager(Uke(1).tirsdag, Uke(1).onsdag, Dag.Kildehendelse.Søknad)

        assertEquals(Uke(1).tirsdag, sykedager.førsteDag())
        assertEquals(Uke(1).onsdag, sykedager.sisteDag())
    }

    @Test
    fun sluttForStartFeiler() {
        assertThrows<IllegalArgumentException> { ConcreteSykdomstidslinje.sykedager(Uke(1).onsdag, Uke(1).tirsdag, Dag.Kildehendelse.Søknad) }
    }

    @Test
    fun sammeDagErEnDag() {
        val sykedager = ConcreteSykdomstidslinje.sykedager(Uke(1).tirsdag, Uke(1).tirsdag, Dag.Kildehendelse.Søknad)

        assertEquals(Uke(1).tirsdag, sykedager.førsteDag())
        assertEquals(Uke(1).tirsdag, sykedager.sisteDag())
    }

    @Test
    fun mandagTilMandagIkkeSyk() {
        val interval = ConcreteSykdomstidslinje.ikkeSykedager(Uke(1).mandag, Uke(2).mandag, Dag.Kildehendelse.Søknad)

        val dager = interval.flatten()

        assertEquals(8, dager.size)
    }

    @Test
    fun mandagTilMandagSyk() {
        val interval = ConcreteSykdomstidslinje.sykedager(Uke(1).mandag, Uke(2).mandag, Dag.Kildehendelse.Søknad)

        val dager = interval.flatten()
        assertEquals(8, dager.size)
    }

    @Test
    fun mandagTilMandagVirkedagerSyk() {
        val interval = ConcreteSykdomstidslinje.sykedager(Uke(1).mandag, Uke(2).mandag, Dag.Kildehendelse.Søknad)

        val dager = interval.flatten()
        assertEquals(8, dager.size)
    }
}
