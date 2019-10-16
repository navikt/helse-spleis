package no.nav.helse.sykdomstidlinje.test

import no.nav.helse.Testhendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.mandag
import no.nav.helse.testhelpers.onsdag
import no.nav.helse.testhelpers.tirsdag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalArgumentException

internal class DateRangeTest {
    private val sendtSøknad = Testhendelse()

    @Test
    fun påfølgendeSykedager() {
        val sykedager = Sykdomstidslinje.sykedager(1.tirsdag, 1.onsdag, sendtSøknad)

        assertEquals(1.tirsdag, sykedager.startdato())
        assertEquals(1.onsdag, sykedager.sluttdato())
        assertEquals(2, sykedager.antallSykedagerHvorViTellerMedHelg())
    }

    @Test
    fun sluttForStartFeiler() {
        assertThrows<IllegalArgumentException> { Sykdomstidslinje.sykedager(1.onsdag, 1.tirsdag, sendtSøknad) }
    }

    @Test
    fun sammeDagErEnDag() {
        val sykedager = Sykdomstidslinje.sykedager(1.tirsdag, 1.tirsdag, sendtSøknad)

        assertEquals(1.tirsdag, sykedager.startdato())
        assertEquals(1.tirsdag, sykedager.sluttdato())
        assertEquals(1, sykedager.antallSykedagerHvorViTellerMedHelg())
    }

    @Test
    fun mandagTilMandagIkkeSyk() {
        val interval = Sykdomstidslinje.ikkeSykedager(1.mandag, 2.mandag, sendtSøknad)

        assertEquals(0, interval.antallSykedagerHvorViTellerMedHelg())

        val dager = interval.flatten()

        assertEquals(8, dager.size)
    }

    @Test
    fun mandagTilMandagSyk() {
        val interval = Sykdomstidslinje.sykedager(1.mandag, 2.mandag, sendtSøknad)
        assertEquals(8, interval.antallSykedagerHvorViTellerMedHelg())

        val dager = interval.flatten()
        assertEquals(8, dager.size)
    }

    @Test
    fun mandagTilMandagVirkedagerSyk() {
        val interval = Sykdomstidslinje.sykedager(1.mandag, 2.mandag, sendtSøknad)
        assertEquals(6, interval.antallSykedagerHvorViIkkeTellerMedHelg())

        val dager = interval.flatten()
        assertEquals(8, dager.size)
    }
}
