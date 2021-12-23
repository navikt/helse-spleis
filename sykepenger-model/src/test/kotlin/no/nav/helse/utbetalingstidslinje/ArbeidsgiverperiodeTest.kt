package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class ArbeidsgiverperiodeTest {

    @Test
    fun `itererer over periode`() {
        val periode1 = 1.januar til 5.januar
        val periode2 = 10.januar til 18.januar
        val arbeidsgiverperiode = agp(periode1, periode2)
        assertEquals(periode1.toList() + periode2.toList(), arbeidsgiverperiode.toList())
    }

    @Test
    fun `arbeidsgiverperiode er den samme hvis første dag er lik`() {
        assertEquals(agp(1.januar til 5.januar), agp(1.januar til 16.januar))
        assertNotEquals(agp(1.januar til 5.januar), agp(2.januar til 17.januar))
    }

    @Test
    fun `arbeidsgiverperiode anses som like om de slutter på samme dag`() {
        assertTrue(agp(1.januar til 16.januar).sammenlign(listOf(16.januar til 16.januar)))
        assertFalse(agp(1.januar til 16.januar).sammenlign(listOf(16.januar til 17.januar)))
        assertFalse(agp(1.januar til 16.januar).sammenlign(listOf(15.januar til 15.januar)))
    }

    @Test
    fun `arbeidsgiverperiode anses som like om de slutter på fredag eller helg`() {
        assertTrue(agp(12.januar til 27.januar).sammenlign(listOf(11.januar til 26.januar)))
        assertTrue(agp(12.januar til 27.januar).sammenlign(listOf(13.januar til 28.januar)))
        assertFalse(agp(12.januar til 27.januar).sammenlign(listOf(14.januar til 29.januar)))

        assertTrue(agp(11.januar til 26.januar).sammenlign(listOf(12.januar til 27.januar)))
        assertTrue(agp(13.januar til 28.januar).sammenlign(listOf(12.januar til 27.januar)))
        assertFalse(agp(14.januar til 29.januar).sammenlign(listOf(12.januar til 27.januar)))
        assertFalse(agp(14.januar til 29.januar).sammenlign(emptyList()))
    }

    @Test
    fun `dekker hele perioden`() {
        val periode = 2.januar til 5.januar
        val arbeidsgiverperiode = agp(periode)
        assertTrue(arbeidsgiverperiode.dekker(periode))
        assertTrue(arbeidsgiverperiode.dekker(3.januar til 4.januar))
        assertFalse(arbeidsgiverperiode.dekker(2.januar til 6.januar))
        assertFalse(arbeidsgiverperiode.dekker(1.januar til 5.januar))
        assertFalse(arbeidsgiverperiode.dekker(1.januar til 6.januar))
    }

    @Test
    fun `hører til`() {
        val periode = 2.januar til 5.januar
        val arbeidsgiverperiode = agp(periode)
        assertTrue(arbeidsgiverperiode.hørerTil(periode, 5.januar))
        assertTrue(arbeidsgiverperiode.hørerTil(1.januar til 2.januar, 5.januar))
        assertFalse(arbeidsgiverperiode.hørerTil(6.januar til 9.januar, 5.januar))
        assertFalse(arbeidsgiverperiode.hørerTil(1.januar til 1.januar, 5.januar))
        assertTrue(arbeidsgiverperiode.hørerTil(6.januar til 9.januar, 6.januar))
        assertTrue(arbeidsgiverperiode.hørerTil(6.januar til 9.januar, 10.januar))
        assertFalse(arbeidsgiverperiode.hørerTil(11.januar til 12.januar, 10.januar))
    }

    @Test
    fun `inneholder periode`() {
        val periode = 2.januar til 5.januar
        val arbeidsgiverperiode = agp(periode)
        assertTrue(periode in arbeidsgiverperiode)
        assertTrue(3.januar til 4.januar in arbeidsgiverperiode)
        assertTrue(2.januar til 6.januar in arbeidsgiverperiode)
        assertTrue(1.januar til 5.januar in arbeidsgiverperiode)
        assertTrue(1.januar til 6.januar in arbeidsgiverperiode)
        assertFalse(1.januar til 1.januar in arbeidsgiverperiode)
        assertFalse(6.januar til 7.januar in arbeidsgiverperiode)
    }

    private fun agp(vararg periode: Periode) = Arbeidsgiverperiode(periode.toList())
}
