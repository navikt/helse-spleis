package no.nav.helse.utbetalingstidslinje

import no.nav.helse.februar
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.A
import no.nav.helse.testhelpers.S
import no.nav.helse.testhelpers.opphold
import no.nav.helse.testhelpers.resetSeed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class VentetidberegnerTest {

    @Test
    fun `ventetiden utgjør de første 16 dagene`() {
        val tidslinje = resetSeed { 17.S }
        val resultat = tidslinje.ventetid()
        assertEquals(1, resultat.size)
        resultat[0].also {
            assertEquals(1.januar til 16.januar, it.dagerUtenAnsvar.single())
            assertEquals(1.januar til 17.januar, it.omsluttendePeriode)
            assertTrue(it.ferdigAvklart)
        }
    }

    @Test
    fun `ventetiden utgjør de første 16 dagene - etterfølges av 15 oppholdsdager`() {
        val tidslinje = resetSeed { 17.S + 15.A }
        val resultat = tidslinje.ventetid()
        assertEquals(1, resultat.size)
        resultat[0].also {
            assertEquals(1.januar til 16.januar, it.dagerUtenAnsvar.single())
            assertEquals(1.januar til 1.februar, it.omsluttendePeriode)
            assertTrue(it.ferdigAvklart)
        }
    }

    @Test
    fun `ventetiden utgjør de første 16 dagene - etterfølges av 16 oppholdsdager`() {
        val tidslinje = resetSeed { 17.S + 16.A }
        val resultat = tidslinje.ventetid()
        assertEquals(1, resultat.size)
        resultat[0].also {
            assertEquals(1.januar til 16.januar, it.dagerUtenAnsvar.single())
            assertEquals(1.januar til 1.februar, it.omsluttendePeriode)
            assertTrue(it.ferdigAvklart)
        }
    }

    @Test
    fun `ventetiden utgjør de første 16 dagene - start på lørdag`() {
        val tidslinje = resetSeed(frøDato = 6.januar) { 17.S }
        val resultat = tidslinje.ventetid()
        assertEquals(1, resultat.size)
        resultat[0].also {
            assertEquals(6.januar til 21.januar, it.dagerUtenAnsvar.single())
            assertEquals(6.januar til 22.januar, it.omsluttendePeriode)
            assertTrue(it.ferdigAvklart)
        }
    }

    @Test
    fun `ventetiden er ikke ferdig hvis det er nøyaktig 16 dager`() {
        val tidslinje = resetSeed { 16.S }
        val resultat = tidslinje.ventetid()
        assertEquals(1, resultat.size)
        resultat[0].also {
            assertEquals(1.januar til 16.januar, it.dagerUtenAnsvar.single())
            assertEquals(1.januar til 16.januar, it.omsluttendePeriode)
            assertFalse(it.ferdigAvklart)
        }
    }

    @Test
    fun `ventetiden er ikke ferdig før det er utbetalt sykepenger - ventetiden slutter på fredag`() {
        val tidslinje = resetSeed(frøDato = 4.januar) { 16.S }
        val resultat = tidslinje.ventetid()
        assertEquals(1, resultat.size)
        resultat[0].also {
            assertEquals(4.januar til 19.januar, it.dagerUtenAnsvar.single())
            assertEquals(4.januar til 19.januar, it.omsluttendePeriode)
            assertFalse(it.ferdigAvklart)
        }
    }

    @Test
    fun `ventetiden er ikke ferdig før det er utbetalt sykepenger - ventetiden slutter på lørdag`() {
        val tidslinje = resetSeed(frøDato = 4.januar) { 17.S }
        val resultat = tidslinje.ventetid()
        assertEquals(1, resultat.size)
        resultat[0].also {
            assertEquals(4.januar til 19.januar, it.dagerUtenAnsvar.single())
            assertEquals(4.januar til 20.januar, it.omsluttendePeriode)
            assertFalse(it.ferdigAvklart)
        }
    }

    @Test
    fun `ventetiden er ikke ferdig før det er utbetalt sykepenger - ventetiden slutter på søndag`() {
        val tidslinje = resetSeed(frøDato = 4.januar) { 18.S }
        val resultat = tidslinje.ventetid()
        assertEquals(1, resultat.size)
        resultat[0].also {
            assertEquals(4.januar til 19.januar, it.dagerUtenAnsvar.single())
            assertEquals(4.januar til 21.januar, it.omsluttendePeriode)
            assertFalse(it.ferdigAvklart)
        }
    }

    @Test
    fun `ventetiden er ikke ferdig før det er utbetalt sykepenger - ventetiden slutter på lørdag (helg ukjent)`() {
        val tidslinje = resetSeed(frøDato = 4.januar) { 16.S + 2.opphold + 1.S }
        val resultat = tidslinje.ventetid()
        assertEquals(1, resultat.size)
        resultat[0].also {
            assertEquals(4.januar til 19.januar, it.dagerUtenAnsvar.single())
            assertEquals(4.januar til 22.januar, it.omsluttendePeriode)
            assertTrue(it.ferdigAvklart)
        }
    }

    @Test
    fun `ventetiden er ikke ferdig før det er utbetalt sykepenger - ventetiden slutter på søndag (helg ukjent)`() {
        val tidslinje = resetSeed(frøDato = 4.januar) { 17.S + 1.opphold + 1.S }
        val resultat = tidslinje.ventetid()
        assertEquals(1, resultat.size)
        resultat[0].also {
            assertEquals(4.januar til 19.januar, it.dagerUtenAnsvar.single())
            assertEquals(4.januar til 22.januar, it.omsluttendePeriode)
            assertTrue(it.ferdigAvklart)
        }
    }

    @Test
    fun `ventetiden er ikke ferdig før det er utbetalt sykepenger - ventetiden slutter på mandag`() {
        val tidslinje = resetSeed(frøDato = 4.januar) { 19.S }
        val resultat = tidslinje.ventetid()
        assertEquals(1, resultat.size)
        resultat[0].also {
            assertEquals(4.januar til 19.januar, it.dagerUtenAnsvar.single())
            assertEquals(4.januar til 22.januar, it.omsluttendePeriode)
            assertTrue(it.ferdigAvklart)
        }
    }

    @Test
    fun `starter ny ventetid hvis forrige ventetid på 16 dager slutter på fredag (helg er ukjent)`() {
        val tidslinje = resetSeed(frøDato = 4.januar) { 16.S + 3.opphold + 10.S }
        val resultat = tidslinje.ventetid()
        assertEquals(2, resultat.size)
        resultat[0].also {
            assertEquals(4.januar til 19.januar, it.dagerUtenAnsvar.single())
            assertEquals(4.januar til 21.januar, it.omsluttendePeriode)
            assertFalse(it.ferdigAvklart)
        }
        resultat[1].also {
            assertEquals(23.januar til 1.februar, it.dagerUtenAnsvar.single())
            assertEquals(23.januar til 1.februar, it.omsluttendePeriode)
            assertFalse(it.ferdigAvklart)
        }
    }

    @Test
    fun `starter ny ventetid hvis forrige ventetid på 16 dager slutter på fredag (helg er arbeid)`() {
        val tidslinje = resetSeed(frøDato = 4.januar) { 16.S + 3.A + 10.S }
        val resultat = tidslinje.ventetid()
        assertEquals(2, resultat.size)
        resultat[0].also {
            assertEquals(4.januar til 19.januar, it.dagerUtenAnsvar.single())
            assertEquals(4.januar til 19.januar, it.omsluttendePeriode)
            assertFalse(it.ferdigAvklart)
        }
        resultat[1].also {
            assertEquals(23.januar til 1.februar, it.dagerUtenAnsvar.single())
            assertEquals(23.januar til 1.februar, it.omsluttendePeriode)
            assertFalse(it.ferdigAvklart)
        }
    }

    @Test
    fun `starter ny ventetid hvis forrige ventetid på 16 dager slutter på lørdag (helg er ukjent)`() {
        val tidslinje = resetSeed(frøDato = 4.januar) { 17.S + 2.opphold + 10.S }
        val resultat = tidslinje.ventetid()
        assertEquals(2, resultat.size)
        resultat[0].also {
            assertEquals(4.januar til 19.januar, it.dagerUtenAnsvar.single())
            assertEquals(4.januar til 21.januar, it.omsluttendePeriode)
            assertFalse(it.ferdigAvklart)
        }
        resultat[1].also {
            assertEquals(23.januar til 1.februar, it.dagerUtenAnsvar.single())
            assertEquals(23.januar til 1.februar, it.omsluttendePeriode)
            assertFalse(it.ferdigAvklart)
        }
    }

    @Test
    fun `starter ny ventetid hvis forrige ventetid på 16 dager slutter på lørdag (helg er arbeid)`() {
        val tidslinje = resetSeed(frøDato = 4.januar) { 17.S + 2.A + 10.S }
        val resultat = tidslinje.ventetid()
        assertEquals(2, resultat.size)
        resultat[0].also {
            assertEquals(4.januar til 19.januar, it.dagerUtenAnsvar.single())
            assertEquals(4.januar til 20.januar, it.omsluttendePeriode)
            assertFalse(it.ferdigAvklart)
        }
        resultat[1].also {
            assertEquals(23.januar til 1.februar, it.dagerUtenAnsvar.single())
            assertEquals(23.januar til 1.februar, it.omsluttendePeriode)
            assertFalse(it.ferdigAvklart)
        }
    }

    @Test
    fun `starter ny ventetid hvis forrige ventetid på 16 dager slutter på søndag`() {
        listOf(
            resetSeed(frøDato = 4.januar) { 18.S + 1.opphold + 10.S },
            resetSeed(frøDato = 4.januar) { 18.S + 1.A + 10.S }
        ).forEach { tidslinje ->
            val resultat = tidslinje.ventetid()
            assertEquals(2, resultat.size)
            resultat[0].also {
                assertEquals(4.januar til 19.januar, it.dagerUtenAnsvar.single())
                assertEquals(4.januar til 21.januar, it.omsluttendePeriode)
                assertFalse(it.ferdigAvklart)
            }
            resultat[1].also {
                assertEquals(23.januar til 1.februar, it.dagerUtenAnsvar.single())
                assertEquals(23.januar til 1.februar, it.omsluttendePeriode)
                assertFalse(it.ferdigAvklart)
            }
        }
    }

    @Test
    fun `samme ventetid hvis det er utbetalt sykepenger og inntil 15 dager sammenhengende opphold mellom`() {
        listOf(
            resetSeed { 17.S + 15.opphold + 10.S + 15.opphold + 10.S },
            resetSeed { 17.S + 15.A + 10.S + 15.A + 10.S }
        ).forEach { tidslinje ->
            val resultat = tidslinje.ventetid()
            assertEquals(1, resultat.size)
            resultat[0].also {
                assertEquals(1.januar til 16.januar, it.dagerUtenAnsvar.single())
                assertEquals(1.januar til 8.mars, it.omsluttendePeriode)
                assertTrue(it.ferdigAvklart)
            }
        }
    }

    @Test
    fun `samme ventetid hvis det er utbetalt sykepenger og inntil 15 dager opphold mellom`() {
        listOf(
            resetSeed { 17.S + 15.opphold + 10.S },
            resetSeed { 17.S + 15.A + 10.S }
        ).forEach { tidslinje ->
            val resultat = tidslinje.ventetid()
            assertEquals(1, resultat.size)
            resultat[0].also {
                assertEquals(1.januar til 16.januar, it.dagerUtenAnsvar.single())
                assertEquals(1.januar til 11.februar, it.omsluttendePeriode)
                assertTrue(it.ferdigAvklart)
            }
        }
    }

    @Test
    fun `ny ventetid hvis det er utbetalt sykepenger til og med fredag og det er 15 dager opphold mellom påfølgende mandag og ny periode`() {
        listOf(
            resetSeed(frøDato = 3.januar) { 17.S + 2.opphold + 15.opphold + 10.S },
            resetSeed(frøDato = 3.januar) { 17.S + 2.A + 15.A + 10.S }
        ).forEach { tidslinje ->
            val resultat = tidslinje.ventetid()
            assertEquals(2, resultat.size)
            resultat[0].also {
                assertEquals(3.januar til 18.januar, it.dagerUtenAnsvar.single())
                assertEquals(3.januar til 3.februar, it.omsluttendePeriode)
                assertTrue(it.ferdigAvklart)
            }
            resultat[1].also {
                assertEquals(6.februar til 15.februar, it.dagerUtenAnsvar.single())
                assertEquals(6.februar til 15.februar, it.omsluttendePeriode)
                assertFalse(it.ferdigAvklart)
            }
        }
    }

    @Test
    fun `ny ventetid hvis det er utbetalt sykepenger og mer enn 15 dager opphold mellom`() {
        listOf(
            resetSeed { 17.S + 16.opphold + 10.S },
            resetSeed { 17.S + 16.A + 10.S }
        ).forEach { tidslinje ->
            val resultat = tidslinje.ventetid()
            assertEquals(2, resultat.size)
            resultat[0].also {
                assertEquals(1.januar til 16.januar, it.dagerUtenAnsvar.single())
                assertEquals(1.januar til 1.februar, it.omsluttendePeriode)
                assertTrue(it.ferdigAvklart)
            }
            resultat[1].also {
                assertEquals(3.februar til 12.februar, it.dagerUtenAnsvar.single())
                assertEquals(3.februar til 12.februar, it.omsluttendePeriode)
                assertFalse(it.ferdigAvklart)
            }
        }
    }

    @Test
    fun `ny ventetid hvis det er opphold i ventetiden`() {
        listOf(
            resetSeed { 10.S + 1.opphold + 10.S },
            resetSeed { 10.S + 1.A + 10.S }
        ).forEach { tidslinje ->
            val resultat = tidslinje.ventetid()
            assertEquals(2, resultat.size)
            resultat[0].also {
                assertEquals(1.januar til 10.januar, it.dagerUtenAnsvar.single())
                assertEquals(1.januar til 10.januar, it.omsluttendePeriode)
                assertFalse(it.ferdigAvklart)
            }
            resultat[1].also {
                assertEquals(12.januar til 21.januar, it.dagerUtenAnsvar.single())
                assertEquals(12.januar til 21.januar, it.omsluttendePeriode)
                assertFalse(it.ferdigAvklart)
            }
        }
    }

    @Test
    fun `samme ventetid hvis det er opphold (ukjent dag) i helg mellom - lørdag og søndag mellom`() {
        val tidslinje = resetSeed { 5.S + 2.opphold + 12.S }
        val resultat = tidslinje.ventetid()
        assertEquals(1, resultat.size)
        resultat[0].also {
            assertEquals(1.januar til 16.januar, it.dagerUtenAnsvar.single())
            assertEquals(1.januar til 19.januar, it.omsluttendePeriode)
            assertTrue(it.ferdigAvklart)
        }
    }

    @Test
    fun `ny ventetid hvis det er opphold (arbeid) i helg mellom - lørdag og søndag mellom`() {
        val tidslinje = resetSeed { 5.S + 2.A + 12.S }
        val resultat = tidslinje.ventetid()
        assertEquals(2, resultat.size)
        resultat[0].also {
            assertEquals(1.januar til 5.januar, it.dagerUtenAnsvar.single())
            assertEquals(1.januar til 5.januar, it.omsluttendePeriode)
            assertFalse(it.ferdigAvklart)
        }
        resultat[1].also {
            assertEquals(8.januar til 19.januar, it.dagerUtenAnsvar.single())
            assertEquals(8.januar til 19.januar, it.omsluttendePeriode)
            assertFalse(it.ferdigAvklart)
        }
    }

    @Test
    fun `samme ventetid hvis det er opphold (ukjent) i helg mellom - lørdag mellom`() {
        val tidslinje = resetSeed { 5.S + 1.opphold + 12.S }
        val resultat = tidslinje.ventetid()
        assertEquals(1, resultat.size)
        resultat[0].also {
            assertEquals(1.januar til 16.januar, it.dagerUtenAnsvar.single())
            assertEquals(1.januar til 18.januar, it.omsluttendePeriode)
            assertTrue(it.ferdigAvklart)
        }
    }

    @Test
    fun `samme ventetid hvis det er opphold i helg mellom - lørdag mellom`() {
        val tidslinje = resetSeed { 5.S + 1.A + 12.S }
        val resultat = tidslinje.ventetid()
        assertEquals(2, resultat.size)
        resultat[0].also {
            assertEquals(1.januar til 5.januar, it.dagerUtenAnsvar.single())
            assertEquals(1.januar til 5.januar, it.omsluttendePeriode)
            assertFalse(it.ferdigAvklart)
        }
        resultat[1].also {
            assertEquals(7.januar til 18.januar, it.dagerUtenAnsvar.single())
            assertEquals(7.januar til 18.januar, it.omsluttendePeriode)
            assertFalse(it.ferdigAvklart)
        }
    }

    @Test
    fun `samme ventetid hvis det er opphold (ukjent dag) i helg mellom - søndag mellom`() {
        val tidslinje = resetSeed { 6.S + 1.opphold + 12.S }
        val resultat = tidslinje.ventetid()
        assertEquals(1, resultat.size)
        resultat[0].also {
            assertEquals(1.januar til 16.januar, it.dagerUtenAnsvar.single())
            assertEquals(1.januar til 19.januar, it.omsluttendePeriode)
            assertTrue(it.ferdigAvklart)
        }
    }

    @Test
    fun `ny ventetid hvis det er opphold (arbeid) i helg mellom - søndag mellom`() {
        val tidslinje = resetSeed { 6.S + 1.A + 12.S }
        val resultat = tidslinje.ventetid()
        assertEquals(2, resultat.size)
        resultat[0].also {
            assertEquals(1.januar til 6.januar, it.dagerUtenAnsvar.single())
            assertEquals(1.januar til 6.januar, it.omsluttendePeriode)
            assertFalse(it.ferdigAvklart)
        }
        resultat[1].also {
            assertEquals(8.januar til 19.januar, it.dagerUtenAnsvar.single())
            assertEquals(8.januar til 19.januar, it.omsluttendePeriode)
            assertFalse(it.ferdigAvklart)
        }
    }

    @Test
    fun `ulik ventetid hvis det er opphold i helg og fredagen`() {
        listOf(
            resetSeed { 4.S + 3.opphold + 12.S },
            resetSeed { 4.S + 3.A + 12.S }
        ).forEach { tidslinje ->
            val resultat = tidslinje.ventetid()
            assertEquals(2, resultat.size)
            resultat[0].also {
                assertEquals(1.januar til 4.januar, it.dagerUtenAnsvar.single())
                assertEquals(1.januar til 4.januar, it.omsluttendePeriode)
                assertFalse(it.ferdigAvklart)
            }
            resultat[1].also {
                assertEquals(8.januar til 19.januar, it.dagerUtenAnsvar.single())
                assertEquals(8.januar til 19.januar, it.omsluttendePeriode)
                assertFalse(it.ferdigAvklart)
            }
        }
    }

    private fun Sykdomstidslinje.ventetid(): List<PeriodeUtenNavAnsvar> {
        val beregner = Ventetidberegner()
        return beregner.result(this)
    }
}
