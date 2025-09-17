package no.nav.helse.utbetalingstidslinje

import no.nav.helse.desember
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
    fun `Nav betaler arbeidsgiverperioden`() {
        val agp = agp(1.januar til 16.januar).utbetalingsdag(1.januar)
        assertTrue(agp.forventerInntekt(1.januar til 16.januar))
    }

    @Test
    fun `har betalt`() {
        assertFalse(agp(1.januar til 16.januar).erFørsteUtbetalingsdagFørEllerLik(1.januar til 17.januar))
        assertTrue(agp(1.januar til 16.januar).utbetalingsdag(17.januar).erFørsteUtbetalingsdagFørEllerLik(1.januar til 17.januar))
        assertTrue(agp(1.januar til 16.januar).utbetalingsdag(17.januar).erFørsteUtbetalingsdagFørEllerLik(1.januar til 18.januar))
        assertFalse(agp(1.januar til 16.januar).utbetalingsdag(18.januar).erFørsteUtbetalingsdagFørEllerLik(1.januar til 17.januar))
    }

    @Test
    fun `helg regnes ikke som betalt`() {
        assertFalse(agp(1.januar til 16.januar).utbetalingsdag(20.januar).erFørsteUtbetalingsdagFørEllerLik(1.januar til 20.januar))
    }

    @Test
    fun `ingen utbetaling dersom perioden er innenfor arbeidsgiverperioden eller før det utbetales noe`() {
        agp(1.januar til 16.januar).utbetalingsdag(23.januar).also { agp ->
            assertFalse(agp.forventerInntekt(31.desember(2017) til 5.januar))
            assertFalse(agp.forventerInntekt(15.januar til 22.januar))
            assertTrue(agp.forventerInntekt(15.januar til 23.januar))
        }
    }

    @Test
    fun `inneholder dager`() {
        val periode = 2.januar til 5.januar
        val arbeidsgiverperiode = agp(periode)
        assertFalse(1.januar in arbeidsgiverperiode)
        assertTrue(2.januar in arbeidsgiverperiode)
        assertTrue(5.januar in arbeidsgiverperiode)
        assertFalse(6.januar in arbeidsgiverperiode) // lørdag
        assertFalse(7.januar in arbeidsgiverperiode) // søndag
        assertFalse(8.januar in arbeidsgiverperiode) // søndag
    }

    @Test
    fun `dekker hele perioden`() {
        val periode = 2.januar til 5.januar
        val arbeidsgiverperiode = agp(periode)
        assertTrue(arbeidsgiverperiode.dekkesAvArbeidsgiver(periode))
        assertTrue(arbeidsgiverperiode.dekkesAvArbeidsgiver(3.januar til 4.januar))
        assertTrue(arbeidsgiverperiode.dekkesAvArbeidsgiver(2.januar til 6.januar))
        assertFalse(arbeidsgiverperiode.dekkesAvArbeidsgiver(2.januar til 8.januar))
        assertTrue(arbeidsgiverperiode.dekkesAvArbeidsgiver(1.januar til 5.januar))
        assertTrue(arbeidsgiverperiode.dekkesAvArbeidsgiver(6.januar til 7.januar))
        assertTrue(arbeidsgiverperiode.dekkesAvArbeidsgiver(1.januar til 6.januar))
        assertFalse(arbeidsgiverperiode.dekkesAvArbeidsgiver(1.januar til 8.januar))
        assertFalse(arbeidsgiverperiode.dekkesAvArbeidsgiver(1.januar til 1.januar))
    }

    @Test
    fun `hører til`() {
        val periode = 2.januar til 5.januar
        val arbeidsgiverperiode = agp(periode)
        assertTrue(arbeidsgiverperiode.hørerTil(periode))
        assertTrue(arbeidsgiverperiode.hørerTil(1.januar til 2.januar))
        assertFalse(arbeidsgiverperiode.hørerTil(6.januar til 9.januar))
        assertFalse(arbeidsgiverperiode.hørerTil(1.januar til 1.januar))
        arbeidsgiverperiode.kjentDag(6.januar)
        assertTrue(arbeidsgiverperiode.hørerTil(6.januar til 9.januar))
        arbeidsgiverperiode.kjentDag(10.januar)
        assertTrue(arbeidsgiverperiode.hørerTil(7.januar til 9.januar))
        assertFalse(arbeidsgiverperiode.hørerTil(11.januar til 12.januar))
    }

    @Test
    fun `hører til sist kjente`() {
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
