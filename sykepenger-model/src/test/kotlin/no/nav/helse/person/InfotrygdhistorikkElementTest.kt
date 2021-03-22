package no.nav.helse.person

import no.nav.helse.hendelser.til
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class InfotrygdhistorikkElementTest {

    private val kilde = TestEvent.testkilde

    @Test
    fun `lik historikk`() {
        val perioder = listOf(
            Infotrygdhistorikk.Friperiode(1.januar til 31.januar),
            Infotrygdhistorikk.Utbetalingsperiode("orgnr", 1.februar til 28.februar, 100.prosent, 25000.månedlig)
        )
        val inntekter = listOf(
            Infotrygdhistorikk.Inntektsopplysning("orgnr", 1.januar, 25000.månedlig, true)
        )
        val arbeidskategorikoder = mapOf(
            "01" to 1.januar
        )
        assertEquals(historikkelement().hashCode(), historikkelement().hashCode())
        assertNotEquals(historikkelement().hashCode(), historikkelement(perioder).hashCode())
        assertEquals(historikkelement(perioder).hashCode(), historikkelement(perioder).hashCode())
        assertEquals(historikkelement(inntekter = inntekter).hashCode(), historikkelement(inntekter = inntekter).hashCode())
        assertNotEquals(historikkelement(perioder, inntekter).hashCode(), historikkelement(inntekter = inntekter).hashCode())
        assertEquals(historikkelement(perioder, inntekter, arbeidskategorikoder).hashCode(), historikkelement(perioder, inntekter, arbeidskategorikoder).hashCode())
        assertNotEquals(historikkelement(perioder, inntekter).hashCode(), historikkelement(perioder, inntekter, arbeidskategorikoder).hashCode())
    }

    @Test
    fun `like perioder`() {
        val ferie = Infotrygdhistorikk.Friperiode(1.januar til 31.januar)
        val ukjent = Infotrygdhistorikk.Ukjent(1.januar til 31.januar)
        val utbetalingAG1 = Infotrygdhistorikk.Utbetalingsperiode("ag1", 1.februar til 28.februar, 100.prosent, 25000.månedlig)
        val utbetalingAG2 = Infotrygdhistorikk.Utbetalingsperiode("ag2", 1.februar til 28.februar, 100.prosent, 25000.månedlig)
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
    fun `utbetalingstidslinje - ferie`() {
        val ferie = Infotrygdhistorikk.Friperiode(1.januar til 10.januar)
        val inspektør = UtbetalingstidslinjeInspektør(ferie.utbetalingstidslinje())
        assertEquals(10, inspektør.fridagTeller)
    }

    @Test
    fun `utbetalingstidslinje - ukjent`() {
        val ferie = Infotrygdhistorikk.Ukjent(1.januar til 10.januar)
        val inspektør = UtbetalingstidslinjeInspektør(ferie.utbetalingstidslinje())
        assertEquals(0, inspektør.size)
    }

    @Test
    fun `utbetalingstidslinje - utbetaling`() {
        val utbetaling = Infotrygdhistorikk.Utbetalingsperiode("ag1", 1.januar til 10.januar, 100.prosent, 25000.månedlig)
        val inspektør = UtbetalingstidslinjeInspektør(utbetaling.utbetalingstidslinje())
        assertEquals(8, inspektør.navDagTeller)
        assertEquals(2, inspektør.navHelgDagTeller)
    }

    @Test
    fun `sykdomstidslinje - ferie`() {
        val ferie = Infotrygdhistorikk.Friperiode(1.januar til 10.januar)
        val inspektør = SykdomstidslinjeInspektør(ferie.sykdomstidslinje(kilde))
        assertTrue(inspektør.dager.values.all { it is Dag.Feriedag })
        assertEquals(10, inspektør.dager.size)
    }

    @Test
    fun `sykdomstidslinje - ukjent`() {
        val ferie = Infotrygdhistorikk.Ukjent(1.januar til 10.januar)
        val inspektør = SykdomstidslinjeInspektør(ferie.sykdomstidslinje(kilde))
        assertTrue(inspektør.dager.isEmpty())
    }

    @Test
    fun `sykdomstidslinje - utbetaling`() {
        val utbetaling = Infotrygdhistorikk.Utbetalingsperiode("ag1", 1.januar til 10.januar, 100.prosent, 25000.månedlig)
        val inspektør = SykdomstidslinjeInspektør(utbetaling.sykdomstidslinje(kilde))
        assertTrue(inspektør.dager.values.all { it is Dag.Sykedag || it is Dag.SykHelgedag })
        assertEquals(10, inspektør.dager.size)
    }

    private fun historikkelement(
        perioder: List<Infotrygdhistorikk.Infotrygdperiode> = emptyList(),
        inntekter: List<Infotrygdhistorikk.Inntektsopplysning> = emptyList(),
        arbeidskategorikoder: Map<String, LocalDate> = emptyMap(),
        tidsstempel: LocalDateTime = LocalDateTime.now()
    ) =
        Infotrygdhistorikk.Element.opprett(
            tidsstempel = tidsstempel,
            perioder = perioder,
            inntekter = inntekter,
            arbeidskategorikoder = arbeidskategorikoder
        )
}
