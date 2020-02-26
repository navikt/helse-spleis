package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Søknad
import no.nav.helse.testhelpers.Uke
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class DateRangeTest {
    @Test
    fun påfølgendeSykedager() {
        val sykedager = ConcreteSykdomstidslinje.sykedager(Uke(1).tirsdag, Uke(1).onsdag, 100.0, Søknad.SøknadDagFactory)

        assertEquals(Uke(1).tirsdag, sykedager.førsteDag())
        assertEquals(Uke(1).onsdag, sykedager.sisteDag())
    }

    @Test
    fun sluttForStartFeiler() {
        assertThrows<IllegalArgumentException> { ConcreteSykdomstidslinje.sykedager(Uke(1).onsdag, Uke(1).tirsdag, 100.0,  Søknad.SøknadDagFactory) }
    }

    @Test
    fun sammeDagErEnDag() {
        val sykedager = ConcreteSykdomstidslinje.sykedager(Uke(1).tirsdag, Uke(1).tirsdag, 100.0, Søknad.SøknadDagFactory)

        assertEquals(Uke(1).tirsdag, sykedager.førsteDag())
        assertEquals(Uke(1).tirsdag, sykedager.sisteDag())
    }

    @Test
    fun mandagTilMandagIkkeSyk() {
        val interval = ConcreteSykdomstidslinje.ikkeSykedager(Uke(1).mandag, Uke(2).mandag, Søknad.SøknadDagFactory)

        val dager = interval.flatten()

        assertEquals(8, dager.size)
    }

    @Test
    fun mandagTilMandagSyk() {
        val interval = ConcreteSykdomstidslinje.sykedager(Uke(1).mandag, Uke(2).mandag, 100.0, Søknad.SøknadDagFactory)

        val dager = interval.flatten()
        assertEquals(8, dager.size)
    }

    @Test
    fun mandagTilMandagVirkedagerSyk() {
        val interval = ConcreteSykdomstidslinje.sykedager(Uke(1).mandag, Uke(2).mandag, 100.0, Søknad.SøknadDagFactory)

        val dager = interval.flatten()
        assertEquals(8, dager.size)
    }
}
