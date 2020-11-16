package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.til
import no.nav.helse.person.Periodetype.*
import no.nav.helse.testhelpers.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class HistoriePeriodetypeTest : HistorieTest() {

    @Test
    fun `infotrygd - gap - spleis - gap - infotrygd - spleis - spleis`() {
        historie(refusjon(1.januar, 31.januar), refusjon(9.april, 30.april))
        historie.add(AG1, sykedager(1.mars, 30.mars))
        historie.add(AG1, sykedager(1.mai, 31.mai))
        historie.add(AG1, sykedager(1.juni, 30.juni))

        assertFalse(historie.forlengerInfotrygd(AG1, 1.mars til 31.mars))
        assertEquals(FØRSTEGANGSBEHANDLING, historie.periodetype(AG1, 1.mars til 31.mars))

        assertTrue(historie.forlengerInfotrygd(AG1, 1.mai til 31.mai))
        assertEquals(OVERGANG_FRA_IT, historie.periodetype(AG1, 1.mai til 31.mai))

        assertTrue(historie.forlengerInfotrygd(AG1, 1.juni til 30.juni))
        assertEquals(INFOTRYGDFORLENGELSE, historie.periodetype(AG1, 1.juni til 30.juni))
    }

    @Test
    fun `infotrygd - spleis - spleis`() {
        historie(refusjon(1.januar, 31.januar))
        historie.add(AG1, sykedager(1.februar, 28.februar))
        historie.add(AG1, sykedager(1.mars, 31.mars))

        assertTrue(historie.forlengerInfotrygd(AG1, 1.februar til 28.februar))
        assertEquals(OVERGANG_FRA_IT, historie.periodetype(AG1, 1.februar til 28.februar))
        assertEquals(INFOTRYGDFORLENGELSE, historie.periodetype(AG1, 1.mars til 31.mars))
    }

    @Test
    fun `infotrygd - spleis - infotrygd - spleis`() {
        historie(refusjon(1.januar, 31.januar), refusjon(1.mars, 31.mars))
        historie.add(AG1, navdager(1.februar, 28.februar))
        historie.add(AG1, sykedager(1.april, 30.april))

        assertTrue(historie.forlengerInfotrygd(AG1, 1.februar til 28.februar))
        assertEquals(OVERGANG_FRA_IT, historie.periodetype(AG1, 1.februar til 28.februar))

        assertTrue(historie.forlengerInfotrygd(AG1, 1.april til 30.april))
        assertEquals(INFOTRYGDFORLENGELSE, historie.periodetype(AG1, 1.april til 30.april))
    }

    @Test
    fun `spleis - infotrygd - spleis`() {
        historie(refusjon(1.februar, 28.februar))
        historie.add(AG1, navdager(1.januar, 31.januar))
        historie.add(AG1, sykedager(1.mars, 31.mars))
        assertEquals(FØRSTEGANGSBEHANDLING, historie.periodetype(AG1, 1.januar til 31.januar))
        assertFalse(historie.forlengerInfotrygd(AG1, 1.mars til 31.mars))
        assertEquals(FORLENGELSE, historie.periodetype(AG1, 1.mars til 31.mars))
    }

    @Test
    fun `infotrygd - spleis`() {
        historie(refusjon(1.januar, 31.januar))
        historie.add(AG1, sykedager(1.februar, 28.februar))
        assertTrue(historie.forlengerInfotrygd(AG1, 1.februar til 28.februar))
        assertEquals(OVERGANG_FRA_IT, historie.periodetype(AG1, 1.februar til 28.februar))
    }

    @Test
    fun `infotrygd - gap - spleis`() {
        historie(refusjon(1.februar, 27.februar))
        historie.add(AG1, sykedager(1.mars, 31.mars))
        assertEquals(FØRSTEGANGSBEHANDLING, historie.periodetype(AG1, 1.mars til 31.mars))
        assertFalse(historie.forlengerInfotrygd(AG1, 1.mars til 31.mars))
    }

    @Test
    fun `spleis - gap - infotrygd - spleis`() {
        historie(refusjon(1.februar, 28.februar))
        historie.add(AG1, navdager(1.januar, 30.januar))
        historie.add(AG1, sykedager(1.mars, 31.mars))
        assertEquals(OVERGANG_FRA_IT, historie.periodetype(AG1, 1.mars til 31.mars))
    }

    @Test
    fun `ubetalt spleis - ubetalt spleis`() {
        historie.add(AG1, sykedager(1.februar, 28.februar))
        historie.add(AG1, sykedager(1.mars, 31.mars))
        assertEquals(FØRSTEGANGSBEHANDLING, historie.periodetype(AG1, 1.januar til 28.februar))
        assertFalse(historie.forlengerInfotrygd(AG1, 1.mars til 31.mars))
        assertEquals(FORLENGELSE, historie.periodetype(AG1, 1.mars til 31.mars))
    }

    @Test
    fun `spleis - ubetalt spleis - ubetalt spleis`() {
        historie.add(AG1, navdager(1.januar, 31.januar))
        historie.add(AG1, sykedager(1.februar, 28.februar))
        historie.add(AG1, sykedager(1.mars, 31.mars))
        assertEquals(FØRSTEGANGSBEHANDLING, historie.periodetype(AG1, 1.januar til 31.januar))
        assertFalse(historie.forlengerInfotrygd(AG1, 1.februar til 28.februar))
        assertEquals(FORLENGELSE, historie.periodetype(AG1, 1.februar til 28.februar))
        assertFalse(historie.forlengerInfotrygd(AG1, 1.mars til 31.mars))
        assertEquals(FORLENGELSE, historie.periodetype(AG1, 1.mars til 31.mars))
    }

    @Test
    fun `infotrygd AG1 - infotrygdferie AG1 - spleis AG2`() {
        historie(refusjon(1.januar, 31.januar), ferie(1.februar, 10.februar))
        historie.add(AG2, sykedager(11.februar, 28.februar))
        assertFalse(historie.forlengerInfotrygd(AG2, 11.februar til 28.februar))
    }
}
