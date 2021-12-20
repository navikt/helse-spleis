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
