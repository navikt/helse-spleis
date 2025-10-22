package no.nav.helse.utbetalingstidslinje

import no.nav.helse.februar
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.S
import no.nav.helse.testhelpers.opphold
import no.nav.helse.testhelpers.resetSeed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class VentetidberegnerTest {

    @Test
    fun `ventetiden utgjør de første 16 dagene`() {
        val tidslinje = resetSeed { 17.S }
        val resultat = tidslinje.ventetid()
        assertEquals(1, resultat.size)
        resultat[0].also {
            assertEquals(1.januar til 16.januar, it.periode)
            assertTrue(it.ferdigAvklart)
        }
    }

    @Test
    fun `ventetiden utgjør de første 16 dagene - start på lørdag`() {
        val tidslinje = resetSeed(frøDato = 6.januar) { 17.S }
        val resultat = tidslinje.ventetid()
        assertEquals(1, resultat.size)
        resultat[0].also {
            assertEquals(6.januar til 21.januar, it.periode)
            assertTrue(it.ferdigAvklart)
        }
    }

    @Test
    fun `ventetiden er ikke ferdig hvis det er nøyaktig 16 dager`() {
        val tidslinje = resetSeed { 16.S }
        val resultat = tidslinje.ventetid()
        assertEquals(1, resultat.size)
        resultat[0].also {
            assertEquals(1.januar til 16.januar, it.periode)
            assertFalse(it.ferdigAvklart)
        }
    }

    @Test
    fun `ventetiden er ikke ferdig før det er utbetalt sykepenger - ventetiden slutter på fredag`() {
        val tidslinje = resetSeed(frøDato = 4.januar) { 16.S }
        val resultat = tidslinje.ventetid()
        assertEquals(1, resultat.size)
        resultat[0].also {
            assertEquals(4.januar til 19.januar, it.periode)
            assertFalse(it.ferdigAvklart)
        }
    }

    @Test
    fun `ventetiden er ikke ferdig før det er utbetalt sykepenger - ventetiden slutter på lørdag`() {
        val tidslinje = resetSeed(frøDato = 4.januar) { 17.S }
        val resultat = tidslinje.ventetid()
        assertEquals(1, resultat.size)
        resultat[0].also {
            assertEquals(4.januar til 19.januar, it.periode)
            assertFalse(it.ferdigAvklart)
        }
    }

    @Test
    fun `ventetiden er ikke ferdig før det er utbetalt sykepenger - ventetiden slutter på søndag`() {
        val tidslinje = resetSeed(frøDato = 4.januar) { 18.S }
        val resultat = tidslinje.ventetid()
        assertEquals(1, resultat.size)
        resultat[0].also {
            assertEquals(4.januar til 19.januar, it.periode)
            assertFalse(it.ferdigAvklart)
        }
    }

    @Test
    fun `ventetiden er ikke ferdig før det er utbetalt sykepenger - ventetiden slutter på mandag`() {
        val tidslinje = resetSeed(frøDato = 4.januar) { 19.S }
        val resultat = tidslinje.ventetid()
        assertEquals(1, resultat.size)
        resultat[0].also {
            assertEquals(4.januar til 19.januar, it.periode)
            assertTrue(it.ferdigAvklart)
        }
    }

    @Disabled
    @Test
    fun `starter ny ventetid hvis forrige ventetid på 16 dager slutter på fredag`() {
        val tidslinje = resetSeed(frøDato = 4.januar) { 16.S + 3.opphold + 10.S }
        val resultat = tidslinje.ventetid()
        assertEquals(2, resultat.size)
        resultat[0].also {
            assertEquals(4.januar til 19.januar, it.periode)
            assertFalse(it.ferdigAvklart)
        }
        resultat[1].also {
            assertEquals(23.januar til 27.januar, it.periode)
            assertFalse(it.ferdigAvklart)
        }
    }

    @Disabled
    @Test
    fun `starter ny ventetid hvis forrige ventetid på 16 dager slutter på lørdag`() {
        val tidslinje = resetSeed(frøDato = 4.januar) { 17.S + 2.opphold + 10.S }
        val resultat = tidslinje.ventetid()
        assertEquals(2, resultat.size)
        resultat[0].also {
            assertEquals(4.januar til 19.januar, it.periode)
            assertFalse(it.ferdigAvklart)
        }
        resultat[1].also {
            assertEquals(23.januar til 27.januar, it.periode)
            assertFalse(it.ferdigAvklart)
        }
    }

    @Disabled
    @Test
    fun `starter ny ventetid hvis forrige ventetid på 16 dager slutter på søndag`() {
        val tidslinje = resetSeed(frøDato = 4.januar) { 18.S + 1.opphold + 10.S }
        val resultat = tidslinje.ventetid()
        assertEquals(2, resultat.size)
        resultat[0].also {
            assertEquals(4.januar til 19.januar, it.periode)
            assertFalse(it.ferdigAvklart)
        }
        resultat[1].also {
            assertEquals(23.januar til 27.januar, it.periode)
            assertFalse(it.ferdigAvklart)
        }
    }

    @Test
    fun `samme ventetid hvis det er utbetalt sykepenger og inntil 15 dager opphold mellom`() {
        val tidslinje = resetSeed { 17.S + 15.opphold + 10.S }
        val resultat = tidslinje.ventetid()
        assertEquals(1, resultat.size)
        resultat[0].also {
            assertEquals(1.januar til 16.januar, it.periode)
            assertTrue(it.ferdigAvklart)
        }
    }

    @Disabled
    @Test
    fun `ny ventetid hvis det er utbetalt sykepenger til og med fredag og mer enn 15 dager opphold mellom`() {
        val tidslinje = resetSeed(frøDato = 3.januar) { 17.S + 15.opphold + 10.S }
        val resultat = tidslinje.ventetid()
        assertEquals(2, resultat.size)
        resultat[0].also {
            assertEquals(3.januar til 18.januar, it.periode)
            assertTrue(it.ferdigAvklart)
        }
        resultat[1].also {
            assertEquals(3.februar til 12.februar, it.periode)
            assertFalse(it.ferdigAvklart)
        }
    }

    @Disabled
    @Test
    fun `ny ventetid hvis det er utbetalt sykepenger og mer enn 15 dager opphold mellom`() {
        val tidslinje = resetSeed { 17.S + 16.opphold + 10.S }
        val resultat = tidslinje.ventetid()
        assertEquals(2, resultat.size)
        resultat[0].also {
            assertEquals(1.januar til 16.januar, it.periode)
            assertTrue(it.ferdigAvklart)
        }
        resultat[1].also {
            assertEquals(3.februar til 12.februar, it.periode)
            assertFalse(it.ferdigAvklart)
        }
    }

    @Disabled
    @Test
    fun `ny ventetid hvis det er opphold i ventetiden`() {
        val tidslinje = resetSeed { 10.S + 1.opphold + 10.S }
        val resultat = tidslinje.ventetid()
        assertEquals(2, resultat.size)
        resultat[0].also {
            assertEquals(1.januar til 10.januar, it.periode)
            assertFalse(it.ferdigAvklart)
        }
        resultat[1].also {
            assertEquals(12.januar til 21.januar, it.periode)
            assertFalse(it.ferdigAvklart)
        }
    }

    @Test
    fun `samme ventetid hvis det er opphold i helg mellom - lørdag og søndag mellom`() {
        val tidslinje = resetSeed { 5.S + 2.opphold + 12.S }
        val resultat = tidslinje.ventetid()
        assertEquals(1, resultat.size)
        resultat[0].also {
            assertEquals(1.januar til 16.januar, it.periode)
            assertTrue(it.ferdigAvklart)
        }
    }

    @Test
    fun `samme ventetid hvis det er opphold i helg mellom - lørdag mellom`() {
        val tidslinje = resetSeed { 5.S + 1.opphold + 12.S }
        val resultat = tidslinje.ventetid()
        assertEquals(1, resultat.size)
        resultat[0].also {
            assertEquals(1.januar til 16.januar, it.periode)
            assertTrue(it.ferdigAvklart)
        }
    }

    @Test
    fun `samme ventetid hvis det er opphold i helg mellom - søndag mellom`() {
        val tidslinje = resetSeed { 6.S + 1.opphold + 12.S }
        val resultat = tidslinje.ventetid()
        assertEquals(1, resultat.size)
        resultat[0].also {
            assertEquals(1.januar til 16.januar, it.periode)
            assertTrue(it.ferdigAvklart)
        }
    }

    @Disabled
    @Test
    fun `ulik ventetid hvis det er opphold i helg og påfølgende mandag`() {
        val tidslinje = resetSeed { 5.S + 3.opphold + 12.S }
        val resultat = tidslinje.ventetid()
        assertEquals(2, resultat.size)
        resultat[0].also {
            assertEquals(1.januar til 7.januar, it.periode)
            assertFalse(it.ferdigAvklart)
        }
        resultat[0].also {
            assertEquals(9.januar til 20.januar, it.periode)
            assertFalse(it.ferdigAvklart)
        }
    }

    @Disabled
    @Test
    fun `ulik ventetid hvis det er opphold i helg og fredagen`() {
        val tidslinje = resetSeed { 4.S + 3.opphold + 12.S }
        val resultat = tidslinje.ventetid()
        assertEquals(2, resultat.size)
        resultat[0].also {
            assertEquals(1.januar til 4.januar, it.periode)
            assertFalse(it.ferdigAvklart)
        }
        resultat[0].also {
            assertEquals(8.januar til 19.januar, it.periode)
            assertFalse(it.ferdigAvklart)
        }
    }

    private fun Sykdomstidslinje.ventetid(): List<Ventetidsavklaring> {
        val beregner = Ventetidberegner()
        return beregner.result(this)
    }
}
