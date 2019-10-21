package no.nav.helse.sykdomstidlinje.test

import no.nav.helse.Testhendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.Uke
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class DateRangeTest {
    private val sendtSøknad = Testhendelse()

    @Test
    fun påfølgendeSykedager() {
        val sykedager = Sykdomstidslinje.sykedager(Uke(1).tirsdag, Uke(1).onsdag, sendtSøknad)

        assertEquals(Uke(1).tirsdag, sykedager.startdato())
        assertEquals(Uke(1).onsdag, sykedager.sluttdato())
        assertEquals(2, sykedager.antallSykedagerHvorViTellerMedHelg())
    }

    @Test
    fun sluttForStartFeiler() {
        assertThrows<IllegalArgumentException> { Sykdomstidslinje.sykedager(Uke(1).onsdag, Uke(1).tirsdag, sendtSøknad) }
    }

    @Test
    fun sammeDagErEnDag() {
        val sykedager = Sykdomstidslinje.sykedager(Uke(1).tirsdag, Uke(1).tirsdag, sendtSøknad)

        assertEquals(Uke(1).tirsdag, sykedager.startdato())
        assertEquals(Uke(1).tirsdag, sykedager.sluttdato())
        assertEquals(1, sykedager.antallSykedagerHvorViTellerMedHelg())
    }

    @Test
    fun mandagTilMandagIkkeSyk() {
        val interval = Sykdomstidslinje.ikkeSykedager(Uke(1).mandag, Uke(2).mandag, sendtSøknad)

        assertEquals(0, interval.antallSykedagerHvorViTellerMedHelg())

        val dager = interval.flatten()

        assertEquals(8, dager.size)
    }

    @Test
    fun mandagTilMandagSyk() {
        val interval = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(2).mandag, sendtSøknad)
        assertEquals(8, interval.antallSykedagerHvorViTellerMedHelg())

        val dager = interval.flatten()
        assertEquals(8, dager.size)
    }

    @Test
    fun mandagTilMandagVirkedagerSyk() {
        val interval = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(2).mandag, sendtSøknad)
        assertEquals(6, interval.antallSykedagerHvorViIkkeTellerMedHelg())

        val dager = interval.flatten()
        assertEquals(8, dager.size)
    }
}
