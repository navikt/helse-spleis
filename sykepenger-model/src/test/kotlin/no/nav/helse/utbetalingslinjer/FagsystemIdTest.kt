package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.hendelser.til
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class FagsystemIdTest : AbstractFagsystemIdTest() {

    @Test
    fun `happy path`() {
        opprettOgUtbetal(0, 5.NAV, 2.HELG, 5.NAV)
        assertTilstander(0, "Initiell", "Ny", "Sendt", "Overført", "Aktiv")
        assertBehov(Behovtype.Utbetaling)
        assertUtbetalingstidslinje(0,1.januar til 12.januar, NavDag::class, NavHelgDag::class)
        assertHistorie(0, 1.januar til 12.januar, NavDag::class, NavHelgDag::class)
    }

    @Test
    fun `utbetaling overført`() {
        opprett(5.NAV, 2.HELG, 5.NAV)
        utbetal(0)
        assertTilstander(0, "Initiell", "Ny", "Sendt")
        assertUtbetalingstidslinje(0,1.januar til 12.januar, NavDag::class, NavHelgDag::class)
        assertTomHistorie(0)
    }

    @Test
    fun `happy path med flere utbetalinger`() {
        opprettOgUtbetal(0, 5.NAV, 2.HELG)
        opprettOgUtbetal(0, 5.NAV, 2.HELG, 5.NAV, 2.HELG)
        assertTilstander(0, "Initiell", "Ny", "Sendt", "Overført", "Aktiv", "Ubetalt",  "Sendt", "Overført", "Aktiv")
        assertBehov(Behovtype.Utbetaling)
        assertUtbetalingstidslinje(0,1.januar til 14.januar, NavDag::class, NavHelgDag::class)
        assertHistorie(0, 1.januar til 14.januar, NavDag::class, NavHelgDag::class)
    }

    @Test
    fun `annullering happy path`() {
        opprettOgUtbetal(0, 5.NAV, 2.HELG, 5.NAV)
        val siste = annuller(0)
        overført(0)
        kvitter(0)
        assertTilstander(0, "Initiell", "Ny", "Sendt", "Overført", "Aktiv", "Sendt", "Overført", "Annullert")
        assertBehov(Behovtype.Utbetaling, siste)
        assertTrue(fagsystemId.erAnnullert())
        assertTomUtbetalingstidslinje(0)
        assertTomHistorie(0)
    }

    @Test
    fun `annullering overført`() {
        opprettOgUtbetal(0, 5.NAV, 2.HELG, 5.NAV)
        val siste = annuller(0)
        assertTilstander(0, "Initiell", "Ny", "Sendt", "Overført", "Aktiv", "Sendt")
        assertBehov(Behovtype.Utbetaling, siste)
        assertFalse(fagsystemId.erAnnullert())
        assertTomUtbetalingstidslinje(0)
        assertHistorie(0, 1.januar til 12.januar, NavDag::class, NavHelgDag::class)
    }

    @Test
    fun `annullering når siste er ubetalt`() {
        opprettOgUtbetal(0, 5.NAV, 2.HELG)
        opprett(5.NAV, 2.HELG, 5.NAV, 2.HELG)
        val siste = annuller(0)
        overført(0)
        kvitter(0)
        assertTilstander(0, "Initiell", "Ny", "Sendt", "Overført", "Aktiv", "Ubetalt", "Sendt", "Overført", "Annullert")
        assertBehov(Behovtype.Utbetaling, siste)
        assertTrue(fagsystemId.erAnnullert())
        assertTomUtbetalingstidslinje(0)
    }

    @Test
    fun `utbetale på annullert fagsystemId`() {
        opprettOgUtbetal(0, 5.NAV, 2.HELG)
        opprett(5.NAV, 2.HELG, 5.NAV, 2.HELG)
        annuller(0)
        overført(0)
        kvitter(0)
        assertThrows<IllegalStateException> { utbetal(0) }
    }

    @Test
    fun `aktiv ubetalt går tilbake til aktiv ved avslag`() {
        opprettOgUtbetal(0, 5.NAV, 2.HELG)
        opprett(5.NAV, 2.HELG, 5.NAV)
        utbetal(0, godkjent = false)
        assertTilstander(0, "Initiell", "Ny", "Sendt", "Overført", "Aktiv", "Ubetalt", "Aktiv")
        assertUtbetalingstidslinje(0,1.januar til 7.januar, NavDag::class, NavHelgDag::class)
        assertHistorie(0, 1.januar til 7.januar, NavDag::class, NavHelgDag::class)
    }

    @Test
    fun `ubetalt etter aktiv`() {
        opprettOgUtbetal(0, 5.NAV, 2.HELG)
        opprett(5.NAV, 2.HELG, 5.NAV)
        assertTilstander(0, "Initiell", "Ny", "Sendt", "Overført", "Aktiv", "Ubetalt")
        assertUtbetalingstidslinje(0, 1.januar til 12.januar, NavDag::class, NavHelgDag::class)
        assertHistorie(0, 1.januar til 7.januar, NavDag::class, NavHelgDag::class)
    }

    @Test
    fun `forsøke annullering med kun ett, ubetalt oppdrag`() {
        opprett(5.NAV, 2.HELG)
        assertThrows<IllegalStateException> { annuller(0) }
    }

    @Test
    fun `forsøke annullering i Ny`() {
        opprett(5.NAV, 2.HELG)
        assertThrows<IllegalStateException> { annuller(0) }
    }

    @Test
    fun `forsøke annullering i Sendt`() {
        opprett(5.NAV, 2.HELG)
        utbetal(0)
        assertThrows<IllegalStateException> { annuller(0) }
    }

    @Test
    fun `forsøke annullering i Overført`() {
        opprett(5.NAV, 2.HELG)
        utbetal(0)
        overført(0)
        assertThrows<IllegalStateException> { annuller(0) }
    }

    @Test
    fun `forsøke å opprette nytt oppdrag når det allerede eksisterer et ikke-utbetalt oppdrag`() {
        opprett(5.NAV, 2.HELG)
        assertThrows<IllegalStateException> { opprett(5.NAV, 2.HELG, 5.NAV) }
    }

    @Test
    fun `ikke-godkjent periode`() {
        opprett(5.NAV, 2.HELG, 5.NAV)
        val siste = utbetal(0, godkjent = false)
        assertTilstander(0, "Initiell", "Ny", "Avvist")
        assertIkkeBehov(Behovtype.Utbetaling, siste)
        assertUtbetalingstidslinje(0, 1.januar til 12.januar, NavDag::class, NavHelgDag::class)
    }

    @Test
    fun `avvist utbetaling`() {
        opprett(5.NAV, 2.HELG, 5.NAV)
        utbetal(0)
        overført(0)
        kvitter(0, UtbetalingHendelse.Oppdragstatus.AVVIST)
        assertTilstander(0, "Initiell", "Ny", "Sendt", "Overført", "Avvist")
        assertUtbetalingstidslinje(0, 1.januar til 12.januar, NavDag::class, NavHelgDag::class)
    }

    @Test
    fun `Ny fagsystemId`() {
        opprett(5.NAV, 2.HELG, 5.NAV)
        assertEquals(1, fagsystemIder.size)
        assertTilstander(0, "Initiell", "Ny")
        assertUtbetalingstidslinje(0, 1.januar til 12.januar, NavDag::class, NavHelgDag::class)
        assertTomHistorie(0)
    }

    @Test
    fun `simulere ny`() {
        opprett(16.AP, 10.NAV)
        fagsystemId.simuler(aktivitetslogg)
        assertTrue(aktivitetslogg.behov().isNotEmpty())
        assertSimuleringsbehov(MAKSDATO, "SPLEIS")
        assertTilstander(0, "Initiell", "Ny")
    }

    @Test
    fun `simulere ubetalt`() {
        opprettOgUtbetal(0, 16.AP, 10.NAV)
        opprett(16.AP, 10.NAV, 10.NAV)
        fagsystemId.simuler(aktivitetslogg)
        assertTilstander(0, "Initiell", "Ny", "Sendt", "Overført", "Aktiv", "Ubetalt")
        assertTrue(aktivitetslogg.behov().isNotEmpty())
        assertSimuleringsbehov(MAKSDATO, "SPLEIS")
    }

    @Test
    fun `Nytt element når fagsystemId'er er forskjellige`() {
        opprettOgUtbetal(0, 1.NAV)
        opprett(1.NAV, 16.AP, 1.NAV)
        assertEquals(2, fagsystemIder.size)
        assertTilstander(0, "Initiell", "Ny", "Sendt", "Overført", "Aktiv")
        assertTilstander(1, "Initiell", "Ny")
        assertUtbetalingstidslinje(0, 1.januar til 1.januar, NavDag::class)
        assertHistorie(0, 1.januar til 1.januar, NavDag::class)
        assertUtbetalingstidslinje(1, 1.januar til 1.januar, NavDag::class, sisteDato = 18.januar)
        assertUtbetalingstidslinje(1, 2.januar til 17.januar, ArbeidsgiverperiodeDag::class, sisteDato = 18.januar)
        assertUtbetalingstidslinje(1, 18.januar til 18.januar, NavDag::class)
        assertTomHistorie(1)
    }

    @Test
    fun `Nytt element ved ny AGP`() {
        opprettOgUtbetal(0, 1.NAV)
        opprett(1.NAV, 1.AP, 1.NAV)
        assertEquals(2, fagsystemIder.size)
        assertTilstander(0, "Initiell", "Ny", "Sendt", "Overført", "Aktiv")
        assertTilstander(1, "Initiell", "Ny")
    }

    @Test
    fun `samme fagsystemId med gap mindre enn 16 dager`() {
        opprettOgUtbetal(0, 1.NAV)
        opprett(1.NAV, 1.ARB, 1.NAV)
        assertEquals(1, fagsystemIder.size)
    }

    @Test
    fun `legger nytt oppdrag til på eksisterende fagsystemId`() {
        opprettOgUtbetal(0, 16.AP, 5.NAV)
        opprett(16.AP, 5.NAV, 5.NAV(1300))
        assertEquals(1, fagsystemIder.size)
        assertTilstander(0, "Initiell", "Ny", "Sendt", "Overført", "Aktiv", "Ubetalt")
    }

    @Test
    fun `Ny fagsystemId når eksisterende fagsystemId er annullert`() {
        opprettOgUtbetal(0, 16.AP, 5.NAV)
        annuller(0)
        overført(0)
        kvitter(0)
        opprett(21.UTELATE, 15.NAV(1300))
        assertEquals(2, fagsystemIder.size)
        assertTrue(fagsystemIder[0].erAnnullert())
        assertFalse(fagsystemIder[1].erAnnullert())
        assertTilstander(0, "Initiell", "Ny", "Sendt", "Overført", "Aktiv", "Sendt", "Overført", "Annullert")
        assertTilstander(1, "Initiell", "Ny")
    }

    @Test
    fun `Ny fagsystemId når eksisterende fagsystemId er avvist`() {
        opprettOgUtbetal(0, 16.AP, 5.NAV, godkjent = false)
        opprett(21.UTELATE, 15.NAV(1300))
        assertEquals(2, fagsystemIder.size)
        inspektør.utbetalingstidslinje(1).also { tidslinje ->
            assertEquals(22.januar, tidslinje.førsteDato())
        }
    }

    @Test
    fun `avslag på første oppdrag fjerner ikke dagene`() {
        opprettOgUtbetal(0, 16.AP, 5.NAV, godkjent = false)
        inspektør.utbetalingstidslinje(0).also { tidslinje ->
            assertEquals(21.januar, tidslinje.sisteDato())
        }
    }

    @Test
    fun `avslag på andre oppdrag fjerner dagene`() {
        opprettOgUtbetal(0, 16.AP, 5.NAV, godkjent = true)
        opprettOgUtbetal(0, 16.AP, 15.NAV, godkjent = false)
        inspektør.utbetalingstidslinje(0).also { tidslinje ->
            assertEquals(21.januar, tidslinje.sisteDato())
        }
    }

    @Test
    fun `godkjent utbetalingsgodkjenning`() {
        opprettOgUtbetal(0, 16.AP, 5.NAV, godkjent = true)
        assertFalse(fagsystemId.erTom())
    }

    @Test
    fun `mapper riktig når det finnes flere fagsystemId'er`() {
        val fagsystemId1 = opprettOgUtbetal(0, 16.AP, 5.NAV)
        val fagsystemId2 = opprettOgUtbetal(1, 16.AP, 5.NAV, 30.ARB, 16.AP, 5.NAV)
        val fagsystemId1Oppdatert = opprett(16.AP, 5.NAV(1300))
        assertEquals(2, fagsystemIder.size)
        assertEquals(fagsystemId1, fagsystemId1Oppdatert)
        assertNotEquals(fagsystemId2, fagsystemId1Oppdatert)
    }

    @Test
    fun `retry før overført`() {
        val aktivitetslogg = Aktivitetslogg()
        opprett(16.AP, 5.NAV)
        utbetal(0)
        assertDoesNotThrow { fagsystemId.prøvIgjen(aktivitetslogg) }
        assertTilstander(0, "Initiell", "Ny", "Sendt")
        assertBehov(Behovtype.Utbetaling, aktivitetslogg)
    }

    @Test
    fun `retry før kvittering`() {
        val aktivitetslogg = Aktivitetslogg()
        opprett(16.AP, 5.NAV)
        utbetal(0)
        overført(0)
        assertDoesNotThrow { fagsystemId.prøvIgjen(aktivitetslogg) }
        assertTilstander(0, "Initiell", "Ny", "Sendt", "Overført")
        assertBehov(Behovtype.Utbetaling, aktivitetslogg)
    }

    @Test
    fun `retry annullering før overført`() {
        val aktivitetslogg = Aktivitetslogg()
        opprettOgUtbetal(0, 16.AP, 5.NAV)
        annuller(0)
        assertDoesNotThrow { fagsystemId.prøvIgjen(aktivitetslogg) }
        assertTilstander(0, "Initiell", "Ny", "Sendt", "Overført", "Aktiv", "Sendt")
        assertBehov(Behovtype.Utbetaling, aktivitetslogg)
    }

    @Test
    fun `retry annullering før kvittering`() {
        val aktivitetslogg = Aktivitetslogg()
        opprettOgUtbetal(0, 16.AP, 5.NAV)
        annuller(0)
        overført(0)
        assertDoesNotThrow { fagsystemId.prøvIgjen(aktivitetslogg) }
        assertTilstander(0, "Initiell", "Ny", "Sendt", "Overført", "Aktiv", "Sendt", "Overført")
        assertBehov(Behovtype.Utbetaling, aktivitetslogg)
    }
}
