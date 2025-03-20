package no.nav.helse.person.infotrygdhistorikk

import no.nav.helse.februar
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.testhelpers.TestEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class UtbetalingsperiodeTest {
    private companion object {
        private val kilde = TestEvent.testkilde
    }

    @Test
    fun `arbeidsgiverutbetaling er ikke lik brukerutbetaling`() {
        val orgnr = "orgnr"
        val fom = 1.januar
        val tom = 2.januar
        val periode1 = ArbeidsgiverUtbetalingsperiode(orgnr, fom, tom)
        val periode2 = PersonUtbetalingsperiode(orgnr, fom, tom)
        assertNotEquals(periode1, periode2)
        assertNotEquals(periode1.hashCode(), periode2.hashCode())
    }

    @Test
    fun `like perioder`() {
        val ferie = Friperiode(1.januar, 31.januar)
        val utbetalingAG1 = ArbeidsgiverUtbetalingsperiode("ag1", 1.februar, 28.februar)
        val utbetalingAG2 = ArbeidsgiverUtbetalingsperiode("ag2", 1.februar, 28.februar)
        assertEquals(ferie, ferie)
        assertNotEquals(ferie, utbetalingAG1)
        assertNotEquals(ferie.hashCode(), utbetalingAG1.hashCode())
        assertNotEquals(utbetalingAG1, utbetalingAG2)
        assertNotEquals(utbetalingAG1.hashCode(), utbetalingAG2.hashCode())
        assertEquals(utbetalingAG1, utbetalingAG1)
        assertEquals(utbetalingAG1.hashCode(), utbetalingAG1.hashCode())
    }

    private fun assertEquals(one: Infotrygdperiode, two: Infotrygdperiode) {
        assertTrue(one.funksjoneltLik(two))
        assertTrue(two.funksjoneltLik(one))
    }

    private fun assertNotEquals(one: Infotrygdperiode, two: Infotrygdperiode) {
        assertFalse(one.funksjoneltLik(two))
        assertFalse(two.funksjoneltLik(one))
    }

    @Test
    fun `utbetalingstidslinje - ferie`() {
        val ferie = Friperiode(1.januar, 10.januar)
        assertEquals(10, ferie.utbetalingstidslinje().inspektør.fridagTeller)
    }

    @Test
    fun `utbetalingstidslinje - utbetaling`() {
        val utbetaling = ArbeidsgiverUtbetalingsperiode("ag1", 1.januar, 10.januar)
        val inspektør = utbetaling.utbetalingstidslinje().inspektør
        assertEquals(8, inspektør.navDagTeller)
        assertEquals(2, inspektør.navHelgDagTeller)
    }

    @Test
    fun `sykdomstidslinje - ferie`() {
        val periode = Friperiode(1.januar, 10.januar)
        val inspektør = periode.sykdomstidslinje(kilde).inspektør
        assertTrue(inspektør.dager.values.all { it is Dag.Feriedag })
        assertEquals(10, inspektør.dager.size)
    }

    @Test
    fun `sykdomstidslinje - utbetaling`() {
        val periode = ArbeidsgiverUtbetalingsperiode("ag1", 1.januar, 10.januar)
        val inspektør = periode.sykdomstidslinje(kilde).inspektør
        assertTrue(inspektør.dager.values.all { it is Dag.Sykedag || it is Dag.SykHelgedag })
        assertEquals(10, inspektør.dager.size)
    }
}
