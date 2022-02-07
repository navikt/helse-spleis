package no.nav.helse.person.infotrygdhistorikk

import no.nav.helse.februar
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.TestEvent
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class UtbetalingsperiodeTest {
    private companion object {
        private val kilde = TestEvent.testkilde
    }

    @Test
    fun `arbeidsgiverutbetaling er ikke lik brukerutbetaling`() {
        val prosent = 100.prosent
        val inntekt1 = 100.daglig(prosent)
        val orgnr = "orgnr"
        val fom = 1.januar
        val tom = 2.januar
        val periode1 = ArbeidsgiverUtbetalingsperiode(orgnr, fom, tom, prosent, inntekt1)
        val periode2 = PersonUtbetalingsperiode(orgnr, fom, tom, prosent, inntekt1)
        assertNotEquals(periode1, periode2)
        assertNotEquals(periode1.hashCode(), periode2.hashCode())
    }

    @Test
    fun `like perioder`() {
        val ferie = Friperiode(1.januar, 31.januar)
        val ukjent = UkjentInfotrygdperiode(1.januar, 31.januar)
        val utbetalingAG1 = ArbeidsgiverUtbetalingsperiode("ag1", 1.februar, 28.februar, 100.prosent, 25000.månedlig)
        val utbetalingAG2 = ArbeidsgiverUtbetalingsperiode("ag2", 1.februar, 28.februar, 100.prosent, 25000.månedlig)
        assertEquals(ferie, ferie)
        assertEquals(ukjent, ukjent)
        assertNotEquals(ferie, ukjent)
        assertNotEquals(ferie.hashCode(), ukjent.hashCode())
        assertNotEquals(ferie, utbetalingAG1)
        assertNotEquals(ferie.hashCode(), utbetalingAG1.hashCode())
        assertNotEquals(utbetalingAG1, utbetalingAG2)
        assertNotEquals(utbetalingAG1.hashCode(), utbetalingAG2.hashCode())
        assertEquals(utbetalingAG1, utbetalingAG1)
        assertEquals(utbetalingAG1.hashCode(), utbetalingAG1.hashCode())
    }

    @Test
    fun `lik periode - avrunding - arbeidsgiver`() {
        val prosent = 30.prosent
        val inntekt1 = 505.daglig(prosent)
        val inntekt2 = inntekt1.reflection { _, månedlig, _, _ -> månedlig }.månedlig
        val periode1 = ArbeidsgiverUtbetalingsperiode("orgnr", 1.januar, 1.januar, prosent, inntekt1)
        val periode2 = ArbeidsgiverUtbetalingsperiode("orgnr", 1.januar, 1.januar, prosent, inntekt2)
        assertNotEquals(inntekt1, inntekt2)
        assertEquals(periode1, periode2)
    }


    @Test
    fun `lik periode - avrunding - bruker`() {
        val prosent = 30.prosent
        val inntekt1 = 505.daglig(prosent)
        val inntekt2 = inntekt1.reflection { _, månedlig, _, _ -> månedlig }.månedlig
        val periode1 = PersonUtbetalingsperiode("orgnr", 1.januar, 1.januar, prosent, inntekt1)
        val periode2 = PersonUtbetalingsperiode("orgnr", 1.januar, 1.januar, prosent, inntekt2)
        assertNotEquals(inntekt1, inntekt2)
        assertEquals(periode1, periode2)
    }

    @Test
    fun `utbetalingstidslinje - ferie`() {
        val ferie = Friperiode(1.januar, 10.januar)
        assertEquals(10, ferie.utbetalingstidslinje().inspektør.fridagTeller)
    }

    @Test
    fun `utbetalingstidslinje - ukjent`() {
        val ferie = UkjentInfotrygdperiode(1.januar, 10.januar)
        assertEquals(0, ferie.utbetalingstidslinje().size)
    }

    @Test
    fun `utbetalingstidslinje - utbetaling`() {
        val utbetaling = ArbeidsgiverUtbetalingsperiode("ag1", 1.januar, 10.januar, 100.prosent, 25000.månedlig)
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
    fun `sykdomstidslinje - ukjent`() {
        val periode = UkjentInfotrygdperiode(1.januar, 10.januar)
        assertTrue(periode.sykdomstidslinje(kilde).inspektør.dager.isEmpty())
    }

    @Test
    fun `sykdomstidslinje - utbetaling`() {
        val periode = ArbeidsgiverUtbetalingsperiode("ag1", 1.januar, 10.januar, 100.prosent, 25000.månedlig)
        val inspektør = periode.sykdomstidslinje(kilde).inspektør
        assertTrue(inspektør.dager.values.all { it is Dag.Sykedag || it is Dag.SykHelgedag })
        assertEquals(10, inspektør.dager.size)
    }

    @Test
    fun `historikk for - ferie`() {
        val periode = Friperiode(1.januar, 10.januar)
        val inspektør = periode.historikkFor("orgnr", Sykdomstidslinje(), kilde).inspektør
        assertTrue(inspektør.dager.values.all { it is Dag.Feriedag })
        assertEquals(10, inspektør.dager.size)
    }

    @Test
    fun `historikk for - ukjent`() {
        val periode = UkjentInfotrygdperiode(1.januar, 10.januar)
        assertTrue(periode.historikkFor("orgnr", Sykdomstidslinje(), kilde).inspektør.dager.isEmpty())
    }

    @Test
    fun `historikk for - utbetaling`() {
        val periode = ArbeidsgiverUtbetalingsperiode("ag1", 1.januar, 10.januar, 100.prosent, 25000.månedlig)
        val inspektør = periode.historikkFor("ag1", Sykdomstidslinje(), kilde).inspektør
        assertTrue(inspektør.dager.values.all { it is Dag.Sykedag || it is Dag.SykHelgedag })
        assertEquals(10, inspektør.dager.size)
    }

    @Test
    fun `historikk for - infotrygddager erstatter spleisdager`() {
        val periode = ArbeidsgiverUtbetalingsperiode("ag1", 5.januar, 10.januar, 100.prosent, 25000.månedlig)
        val inspektør = periode.historikkFor("ag1", Sykdomstidslinje.arbeidsdager(1.januar til 10.januar, kilde), kilde).inspektør
        assertTrue(inspektør.dager.filter { (dato, _) -> dato < 5.januar }.values.all { it is Dag.Arbeidsdag })
        assertTrue(inspektør.dager.filter { (dato, _) -> dato >= 5.januar }.values.all { it is Dag.Sykedag || it is Dag.SykHelgedag })
        assertEquals(10, inspektør.dager.size)
    }

    @Test
    fun `historikk for annet orgnr - utbetaling`() {
        val periode = ArbeidsgiverUtbetalingsperiode("ag1", 1.januar, 10.januar, 100.prosent, 25000.månedlig)
        assertTrue(periode.historikkFor("noe helt annet", Sykdomstidslinje(), kilde).inspektør.dager.isEmpty())
    }
}
