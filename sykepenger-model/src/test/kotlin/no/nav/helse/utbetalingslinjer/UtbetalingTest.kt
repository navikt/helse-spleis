package no.nav.helse.utbetalingslinjer

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.SimuleringResultat
import no.nav.helse.hendelser.til
import no.nav.helse.hendelser.utbetaling.AnnullerUtbetaling
import no.nav.helse.hendelser.utbetaling.Grunnbeløpsregulering
import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse
import no.nav.helse.hendelser.utbetaling.UtbetalingOverført
import no.nav.helse.hendelser.utbetaling.Utbetalingsgodkjenning
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.testhelpers.AP
import no.nav.helse.testhelpers.ARB
import no.nav.helse.testhelpers.FRI
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.UKJ
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingslinjer.Oppdragstatus.AKSEPTERT
import no.nav.helse.utbetalingslinjer.Oppdragstatus.AVVIST
import no.nav.helse.utbetalingslinjer.Oppdragstatus.OVERFØRT
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.aktive
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.tillaterOpprettelseAvUtbetaling
import no.nav.helse.utbetalingstidslinje.MaksimumUtbetalingFilter
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class UtbetalingTest {

    private lateinit var aktivitetslogg: Aktivitetslogg

    private companion object {
        private const val UNG_PERSON_FNR_2018 = "12029240045"
        private const val ORGNUMMER = "987654321"
    }

    @BeforeEach
    internal fun initEach() {
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `nærliggende utbetaling`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV)
        beregnUtbetalinger(tidslinje)
        val utbetaling = opprettUtbetaling(tidslinje)
        assertTrue(utbetaling.harNærliggendeUtbetaling(31.januar til 15.februar))
        assertTrue(utbetaling.harNærliggendeUtbetaling(1.desember(2017) til 20.januar))
        assertTrue(utbetaling.harNærliggendeUtbetaling(1.februar til 15.februar))
        assertTrue(utbetaling.harNærliggendeUtbetaling(15.februar til 5.mars))
        assertFalse(utbetaling.harNærliggendeUtbetaling(1.januar til 15.januar))
    }

    @Test
    fun `nærliggende utbetaling til ferie`() {
        val tidslinje = tidslinjeOf(16.AP, 17.NAV, 28.FRI)
        beregnUtbetalinger(tidslinje)
        val utbetaling = opprettUtbetaling(tidslinje)
        assertTrue(utbetaling.harNærliggendeUtbetaling(1.februar til 15.februar))
    }

    @Test
    fun `utbetalinger med ulik korrelasjonsId kan ikke overlappe`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV)
        beregnUtbetalinger(tidslinje)
        val utbetaling = opprettUtbetaling(tidslinje)
        val utbetaling2 = opprettUtbetaling(tidslinje)
        assertFalse(listOf(utbetaling).tillaterOpprettelseAvUtbetaling(utbetaling2))
    }

    @Test
    fun `utbetalinger med ulik korrelasjonsId kan overlappe hvis det er annullering`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV)
        beregnUtbetalinger(tidslinje)
        val utbetaling = opprettUtbetaling(tidslinje)
        val utbetaling2 = annuller(opprettUtbetaling(tidslinje))
        assertTrue(listOf(utbetaling).tillaterOpprettelseAvUtbetaling(utbetaling2!!))
    }

    @Test
    fun `utbetalinger med samme korrelasjonsId kan overlappe`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV)
        beregnUtbetalinger(tidslinje)
        val utbetaling = opprettUtbetaling(tidslinje)
        val tidslinje2 = tidslinjeOf(16.AP, 30.NAV)
        beregnUtbetalinger(tidslinje2)
        val utbetaling2 = opprettUtbetaling(tidslinje2, tidligere = utbetaling)
        assertTrue(listOf(utbetaling).tillaterOpprettelseAvUtbetaling(utbetaling2))
    }
    @Test
    fun `ikke nærliggende utbetaling til tomme oppdrag`() {
        val tidslinje = tidslinjeOf(31.FRI)
        beregnUtbetalinger(tidslinje)
        val utbetaling = opprettUtbetaling(tidslinje)
        assertFalse(utbetaling.harNærliggendeUtbetaling(31.januar til 15.februar))
        assertFalse(utbetaling.harNærliggendeUtbetaling(1.desember(2017) til 20.januar))
        assertFalse(utbetaling.harNærliggendeUtbetaling(1.februar til 15.februar))
        assertFalse(utbetaling.harNærliggendeUtbetaling(1.januar til 15.januar))
    }

    @Test
    fun `utbetaling starter i Ny`() {
        val tidslinje = tidslinjeOf(16.AP, 17.NAV)
        beregnUtbetalinger(tidslinje)

        val sisteDato = 21.januar
        val utbetaling = Utbetaling.lagUtbetaling(
            emptyList(),
            UNG_PERSON_FNR_2018,
            UUID.randomUUID(),
            ORGNUMMER,
            tidslinje,
            sisteDato,
            aktivitetslogg,
            LocalDate.MAX,
            100,
            148
        )
        assertEquals(Utbetaling.Ny, utbetaling.inspektør.tilstand)
        utbetaling.opprett(aktivitetslogg)
        assertEquals(Utbetaling.Ubetalt, utbetaling.inspektør.tilstand)
    }

    @Test
    fun `utbetalinger inkluderer ikke dager etter siste dato`() {
        val tidslinje = tidslinjeOf(16.AP, 17.NAV)
        beregnUtbetalinger(tidslinje)

        val sisteDato = 21.januar
        val utbetaling = Utbetaling.lagUtbetaling(
            emptyList(),
            UNG_PERSON_FNR_2018,
            UUID.randomUUID(),
            ORGNUMMER,
            tidslinje,
            sisteDato,
            aktivitetslogg,
            LocalDate.MAX,
            100,
            148
        ).also { it.opprett(aktivitetslogg) }
        assertEquals(1.januar til sisteDato, utbetaling.inspektør.utbetalingstidslinje.periode())
        assertEquals(17.januar til sisteDato, utbetaling.inspektør.periode)
    }

    @Test
    fun `periode for annullering`() {
        val tidslinje = tidslinjeOf(16.AP, 5.NAV, 1.FRI, 6.NAV, 1.FRI, 4.NAV)
        beregnUtbetalinger(tidslinje)
        val første = opprettUtbetaling(tidslinje, sisteDato = 21.januar)
        val andre = opprettUtbetaling(tidslinje, første, 28.januar)
        val tredje = opprettUtbetaling(tidslinje, andre, 2.februar)
        val annullering = annuller(tredje)
        no.nav.helse.testhelpers.assertNotNull(annullering)
        val utbetalingInspektør = annullering.inspektør
        assertEquals(første.inspektør.korrelasjonsId, utbetalingInspektør.korrelasjonsId)
        assertEquals(17.januar til 2.februar, utbetalingInspektør.periode)
        assertEquals(16.januar, utbetalingInspektør.arbeidsgiverOppdrag.inspektør.periode.start)
        assertEquals(30.januar, utbetalingInspektør.arbeidsgiverOppdrag.first().inspektør.fom)
        assertEquals(17.januar, utbetalingInspektør.arbeidsgiverOppdrag.first().inspektør.datoStatusFom)
        assertEquals(2.februar, utbetalingInspektør.arbeidsgiverOppdrag.last().inspektør.tom)
    }

    @Test
    fun `forlenger seg ikke på en annullering`() {
        val tidslinje = tidslinjeOf(16.AP, 10.NAV)
        beregnUtbetalinger(tidslinje)
        val første = opprettUtbetaling(tidslinje)
        val annullering = annuller(første)
        no.nav.helse.testhelpers.assertNotNull(annullering)
        val andre = opprettUtbetaling(tidslinje, annullering)
        assertNotEquals(første.inspektør.korrelasjonsId, andre.inspektør.korrelasjonsId)
        assertNotEquals(første.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), andre.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
    }

    @Test
    fun `omgjøre en revurdert periode med opphør`() {
        val tidslinje = tidslinjeOf(16.AP, 10.NAV)
        val ferietidslinje = tidslinjeOf(16.AP, 10.FRI)
        beregnUtbetalinger(tidslinje)
        val første = opprettUtbetaling(tidslinje)
        val andre = opprettUtbetaling(ferietidslinje, første)
        val tredje = opprettUtbetaling(tidslinje, andre)
        assertEquals(første.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), andre.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
        assertEquals(første.inspektør.korrelasjonsId, andre.inspektør.korrelasjonsId)
        assertTrue(andre.inspektør.arbeidsgiverOppdrag[0].erOpphør())
        assertEquals(første.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), tredje.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
        assertEquals(første.inspektør.korrelasjonsId, tredje.inspektør.korrelasjonsId)
        assertEquals(17.januar, tredje.inspektør.arbeidsgiverOppdrag.first().inspektør.fom)
        assertEquals(26.januar, tredje.inspektør.arbeidsgiverOppdrag.last().inspektør.tom)
        assertEquals(16.januar, tredje.inspektør.arbeidsgiverOppdrag.inspektør.periode.start)
    }

    @Test
    fun `forlenge en utbetaling uten utbetaling`() {
        val tidslinje = tidslinjeOf(16.AP, 10.NAV)
        beregnUtbetalinger(tidslinje)
        val første = opprettUtbetaling(tidslinje.kutt(16.januar))
        val andre = opprettUtbetaling(tidslinje, første)
        assertEquals(første.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), andre.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
        assertEquals(første.inspektør.korrelasjonsId, andre.inspektør.korrelasjonsId)
        assertTrue(første.inspektør.arbeidsgiverOppdrag.isEmpty())
        assertTrue(andre.inspektør.arbeidsgiverOppdrag.isNotEmpty())
        assertEquals(første.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), andre.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
        assertEquals(første.inspektør.korrelasjonsId, andre.inspektør.korrelasjonsId)
        assertEquals(Endringskode.NY, andre.inspektør.arbeidsgiverOppdrag.inspektør.endringskode)
        assertEquals(Endringskode.NY, andre.inspektør.arbeidsgiverOppdrag[0].inspektør.endringskode)
        assertEquals(17.januar, andre.inspektør.arbeidsgiverOppdrag[0].inspektør.fom)
        assertEquals(26.januar, andre.inspektør.arbeidsgiverOppdrag[0].inspektør.tom)
    }

    @Test
    fun `forlenger seg på en revurdert periode med opphør`() {
        val tidslinje = tidslinjeOf(16.AP, 10.NAV)
        val ferietidslinje = tidslinjeOf(16.AP, 10.FRI)
        beregnUtbetalinger(tidslinje)
        val første = opprettUtbetaling(tidslinje)
        val andre = opprettUtbetaling(ferietidslinje, første)
        val tredje = opprettUtbetaling(beregnUtbetalinger(ferietidslinje + tidslinjeOf(10.NAV, startDato = 27.januar)), andre)
        assertEquals(første.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), andre.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
        assertEquals(første.inspektør.korrelasjonsId, andre.inspektør.korrelasjonsId)
        assertTrue(andre.inspektør.arbeidsgiverOppdrag[0].erOpphør())
        assertEquals(første.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), tredje.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
        assertEquals(første.inspektør.korrelasjonsId, tredje.inspektør.korrelasjonsId)
        assertEquals(27.januar, tredje.inspektør.arbeidsgiverOppdrag.first().inspektør.fom)
        assertNull(tredje.inspektør.arbeidsgiverOppdrag.first().inspektør.datoStatusFom)
        assertEquals(5.februar, tredje.inspektør.arbeidsgiverOppdrag.last().inspektør.tom)
        assertEquals(16.januar, tredje.inspektør.arbeidsgiverOppdrag.inspektør.sisteArbeidsgiverdag)
        assertEquals(16.januar, tredje.inspektør.arbeidsgiverOppdrag.inspektør.periode.start)
    }

    @Test
    fun nettoBeløp() {
        val tidslinje = tidslinjeOf(11.NAV)
        beregnUtbetalinger(tidslinje)
        val første = opprettUtbetaling(tidslinje, sisteDato = 7.januar)
        val andre = opprettUtbetaling(tidslinje, første)

        assertEquals(6000, første.inspektør.arbeidsgiverOppdrag.inspektør.nettoBeløp)
        assertEquals(4800, andre.inspektør.arbeidsgiverOppdrag.inspektør.nettoBeløp)
    }

    @Test
    fun `sorterer etter når fagsystemIDen ble oppretta`() {
        val tidslinje = tidslinjeOf(
            16.AP, 3.NAV, 5.NAV(1200, 50.0), 7.FRI, 16.AP, 1.NAV,
            startDato = 1.januar(2020)
        )

        beregnUtbetalinger(tidslinje)

        val første = opprettUtbetaling(tidslinje.kutt(19.januar(2020)))
        val andre = opprettUtbetaling(tidslinje.kutt(24.januar(2020)), tidligere = første)
        val tredje = opprettUtbetaling(tidslinje.kutt(18.februar(2020)), tidligere = andre)
        val andreAnnullert = annuller(andre)
        no.nav.helse.testhelpers.assertNotNull(andreAnnullert)
        godkjenn(andreAnnullert)
        assertEquals(listOf(andre, tredje), listOf(tredje, andre, første).aktive())
        assertEquals(listOf(tredje), listOf(andreAnnullert, tredje, andre, første).aktive())
    }

    @Test
    fun `setter refFagsystemId og refDelytelseId når en linje peker på en annen`() {
        val tidslinje = tidslinjeOf(
            16.AP, 3.NAV, 5.NAV(1200, 50.0),
            startDato = 1.januar(2020)
        )

        beregnUtbetalinger(tidslinje)

        val tidligere = opprettUtbetaling(tidslinje.kutt(19.januar(2020)))
        val utbetaling = opprettUtbetaling(tidslinje.kutt(24.januar(2020)), tidligere = tidligere)

        assertEquals(tidligere.inspektør.korrelasjonsId, utbetaling.inspektør.korrelasjonsId)
        utbetaling.inspektør.arbeidsgiverOppdrag.inspektør.also { inspektør ->
            assertEquals(2, inspektør.antallLinjer())
            assertNull(inspektør.refDelytelseId(0))
            assertNull(inspektør.refFagsystemId(0))
            assertNotNull(inspektør.refDelytelseId(1))
            assertNotNull(inspektør.refFagsystemId(1))
        }
    }

    @Test
    fun `separate utbetalinger`() {
        val tidslinje = tidslinjeOf(
            16.AP,
            9.NAV,
            5.AP,
            30.NAV,
            startDato = 1.januar(2020)
        )

        beregnUtbetalinger(tidslinje)

        val første = opprettUtbetaling(tidslinje.kutt(19.januar(2020)))
        val andre = opprettUtbetaling(tidslinje, tidligere = første)

        val inspektør1 = første.inspektør.arbeidsgiverOppdrag.inspektør
        val inspektør2 = andre.inspektør.arbeidsgiverOppdrag.inspektør
        assertNotEquals(første.inspektør.korrelasjonsId, andre.inspektør.korrelasjonsId)
        assertEquals(1, inspektør1.antallLinjer())
        assertEquals(1, inspektør2.antallLinjer())
        assertNull(inspektør1.refDelytelseId(0))
        assertNull(inspektør1.refFagsystemId(0))
        assertNull(inspektør2.refDelytelseId(0))
        assertNull(inspektør2.refFagsystemId(0))
        assertNotEquals(inspektør1.fagsystemId(), inspektør2.fagsystemId())
    }

    @Test
    fun `kan forkaste uten utbetalinger`() {
        assertTrue(Utbetaling.kanForkastes(emptyList(), emptyList()))
    }


    @Test
    fun `kan forkaste ubetalt utbetaling`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV)
        beregnUtbetalinger(tidslinje)
        val utbetaling = opprettUbetaltUtbetaling(tidslinje)
        assertTrue(Utbetaling.kanForkastes(listOf(utbetaling), listOf(utbetaling)))
    }

    @Test
    fun `kan forkaste underkjent utbetaling`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV)
        beregnUtbetalinger(tidslinje)
        val utbetaling = opprettUbetaltUtbetaling(tidslinje)
        godkjenn(utbetaling, false)
        assertTrue(Utbetaling.kanForkastes(listOf(utbetaling), listOf(utbetaling)))
    }

    @Test
    fun `kan forkaste forkastet utbetaling`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV)
        beregnUtbetalinger(tidslinje)
        val utbetaling = opprettUbetaltUtbetaling(tidslinje)
        utbetaling.forkast(Aktivitetslogg())
        assertTrue(Utbetaling.kanForkastes(listOf(utbetaling), listOf(utbetaling)))
    }

    @Test
    fun `kan ikke forkaste utbetaling i spill`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV)
        beregnUtbetalinger(tidslinje)
        val utbetaling = opprettGodkjentUtbetaling(tidslinje)
        assertFalse(Utbetaling.kanForkastes(listOf(utbetaling), listOf(utbetaling)))
        overfør(utbetaling, utbetaling.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
        assertFalse(Utbetaling.kanForkastes(listOf(utbetaling), listOf(utbetaling)))
        kvittèr(utbetaling, utbetaling.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
        assertFalse(Utbetaling.kanForkastes(listOf(utbetaling), listOf(utbetaling)))
    }

    @Test
    fun `kan ikke forkaste feilet utbetaling`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV)
        beregnUtbetalinger(tidslinje)
        val utbetaling = opprettGodkjentUtbetaling(tidslinje)
        overfør(utbetaling, utbetaling.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
        kvittèr(utbetaling, utbetaling.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), status = AVVIST)
        assertFalse(Utbetaling.kanForkastes(listOf(utbetaling), listOf(utbetaling)))
    }

    @Test
    fun `kan forkaste annullert utbetaling`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV)
        beregnUtbetalinger(tidslinje)
        val utbetaling = opprettUtbetaling(tidslinje)
        val annullering = annuller(utbetaling, utbetaling.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId()) ?: fail { "Kunne ikke annullere" }
        assertTrue(Utbetaling.kanForkastes(listOf(utbetaling), listOf(annullering)))
    }

    @Test
    fun `kan forkaste utbetalt utbetaling dersom den er annullert`() {
        val tidslinje = tidslinjeOf(16.AP, 32.NAV)
        beregnUtbetalinger(tidslinje)
        val utbetaling = opprettUtbetaling(tidslinje.kutt(31.januar))
        val annullert = opprettUtbetaling(tidslinje.kutt(17.februar), tidligere = utbetaling).let {
            annuller(it, it.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
        } ?: fail { "Kunne ikke annullere" }
        assertTrue(Utbetaling.kanForkastes(listOf(utbetaling), listOf(annullert)))
    }

    @Test
    fun `går ikke videre når ett av to oppdrag er overført`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV(dekningsgrunnlag = 1000, refusjonsbeløp = 600))
        beregnUtbetalinger(tidslinje)
        val utbetaling = opprettGodkjentUtbetaling(tidslinje)
        overfør(utbetaling)
        assertEquals(Utbetaling.Sendt, utbetaling.inspektør.tilstand)
    }

    @Test
    fun `går videre når begge oppdragene er overført`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV(dekningsgrunnlag = 1000, refusjonsbeløp = 600))
        beregnUtbetalinger(tidslinje)
        val utbetaling = opprettGodkjentUtbetaling(tidslinje)
        overfør(utbetaling)
        overfør(utbetaling, utbetaling.inspektør.personOppdrag.fagsystemId())
        assertEquals(Utbetaling.Overført, utbetaling.inspektør.tilstand)
    }

    @Test
    fun `går ikke videre når ett av to oppdrag er akseptert`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV(dekningsgrunnlag = 1000, refusjonsbeløp = 600))
        beregnUtbetalinger(tidslinje)
        val utbetaling = opprettGodkjentUtbetaling(tidslinje)
        kvittèr(utbetaling)
        assertEquals(Utbetaling.Sendt, utbetaling.inspektør.tilstand)
    }

    @Test
    fun `går videre når begge oppdragene er akseptert`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV(dekningsgrunnlag = 1000, refusjonsbeløp = 600))
        beregnUtbetalinger(tidslinje)
        val utbetaling = opprettGodkjentUtbetaling(tidslinje)
        kvittèr(utbetaling)
        kvittèr(utbetaling, utbetaling.inspektør.personOppdrag.fagsystemId())
        assertEquals(Utbetaling.Utbetalt, utbetaling.inspektør.tilstand)
    }

    @Test
    fun `utbetalingen er feilet dersom én av oppdragene er avvist 1`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV(dekningsgrunnlag = 1000, refusjonsbeløp = 600))
        beregnUtbetalinger(tidslinje)
        val utbetaling = opprettGodkjentUtbetaling(tidslinje)
        kvittèr(utbetaling, utbetaling.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), AVVIST)
        assertEquals(Utbetaling.UtbetalingFeilet, utbetaling.inspektør.tilstand)
    }

    @Test
    fun `tar imot kvittering på det andre oppdraget selv om utbetalingen har feilet`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV(dekningsgrunnlag = 1000, refusjonsbeløp = 600))
        beregnUtbetalinger(tidslinje)
        val utbetaling = opprettGodkjentUtbetaling(tidslinje)
        kvittèr(utbetaling, utbetaling.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), AVVIST)
        kvittèr(utbetaling, utbetaling.inspektør.personOppdrag.fagsystemId(), AKSEPTERT)
        assertEquals(AVVIST, utbetaling.inspektør.arbeidsgiverOppdrag.inspektør.status())
        assertEquals(AKSEPTERT, utbetaling.inspektør.personOppdrag.inspektør.status())
        assertEquals(Utbetaling.UtbetalingFeilet, utbetaling.inspektør.tilstand)
    }

    @Test
    fun `tar imot overført på det andre oppdraget selv om utbetalingen har feilet`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV(dekningsgrunnlag = 1000, refusjonsbeløp = 600))
        beregnUtbetalinger(tidslinje)
        val utbetaling = opprettGodkjentUtbetaling(tidslinje)
        kvittèr(utbetaling, utbetaling.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), AVVIST)
        overfør(utbetaling, utbetaling.inspektør.personOppdrag.fagsystemId())
        assertEquals(AVVIST, utbetaling.inspektør.arbeidsgiverOppdrag.inspektør.status())
        assertEquals(OVERFØRT, utbetaling.inspektør.personOppdrag.inspektør.status())
        assertEquals(Utbetaling.UtbetalingFeilet, utbetaling.inspektør.tilstand)
    }

    @Test
    fun `utbetalingen er feilet dersom én av oppdragene er avvist 2`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV(dekningsgrunnlag = 1000, refusjonsbeløp = 600))
        beregnUtbetalinger(tidslinje)
        val utbetaling = opprettGodkjentUtbetaling(tidslinje)
        kvittèr(utbetaling, utbetaling.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), AKSEPTERT)
        kvittèr(utbetaling, utbetaling.inspektør.personOppdrag.fagsystemId(), AVVIST)
        assertEquals(Utbetaling.UtbetalingFeilet, utbetaling.inspektør.tilstand)
    }

    @Test
    fun `delvis refusjon`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV(dekningsgrunnlag = 1000, refusjonsbeløp = 600))
        beregnUtbetalinger(tidslinje)
        val utbetaling = opprettUtbetaling(tidslinje)
        assertTrue(utbetaling.harDelvisRefusjon())
    }

    @Test
    fun `annullere delvis refusjon`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV(dekningsgrunnlag = 1000, refusjonsbeløp = 600))
        beregnUtbetalinger(tidslinje)
        val utbetaling = opprettUtbetaling(tidslinje)
        val annullering = annuller(utbetaling)
        no.nav.helse.testhelpers.assertNotNull(annullering)
        assertTrue(annullering.inspektør.arbeidsgiverOppdrag.last().erOpphør())
        assertTrue(annullering.inspektør.personOppdrag.last().erOpphør())
    }

    @Test
    fun `annullere på fagsystemId for personoppdrag`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV(dekningsgrunnlag = 1000, refusjonsbeløp = 0))
        beregnUtbetalinger(tidslinje)
        val utbetaling = opprettUtbetaling(tidslinje)
        val annullering = annuller(utbetaling, utbetaling.inspektør.personOppdrag.fagsystemId())
        assertNull(annullering) { "Det er ikke støttet å annullere på personoppdrag sin fagsystemId pt. Annullering bør skje på utbetalingId" }
    }

    @Test
    fun `annullere utbetaling med full refusjon, så null refusjon`() {
        val tidslinje = tidslinjeOf(16.AP, 5.NAV, 10.NAV(dekningsgrunnlag = 1000, refusjonsbeløp = 600))
        beregnUtbetalinger(tidslinje)
        val første = opprettUtbetaling(tidslinje.kutt(21.januar))
        val andre = opprettUtbetaling(tidslinje, første)
        val annullering = annuller(andre)
        no.nav.helse.testhelpers.assertNotNull(annullering)
        assertTrue(annullering.inspektør.arbeidsgiverOppdrag.last().erOpphør())
        assertTrue(annullering.inspektør.personOppdrag.last().erOpphør())
    }

    @Test
    fun `null refusjon`() {
        val tidslinje = tidslinjeOf(16.AP, 30.NAV(dekningsgrunnlag = 1000, refusjonsbeløp = 0))
        beregnUtbetalinger(tidslinje)
        val første = opprettUtbetaling(tidslinje.kutt(31.januar))
        val andre = opprettUtbetaling(tidslinje, første)
        assertFalse(første.harDelvisRefusjon())
        assertTrue(første.harUtbetalinger())
        assertEquals(første.inspektør.korrelasjonsId, andre.inspektør.korrelasjonsId)
    }

    @Test
    fun `korrelasjonsId er lik på brukerutbetalinger direkte fra Infotrygd`() {
        val tidslinje =
            beregnUtbetalinger(tidslinjeOf(5.UKJ, 26.NAV(dekningsgrunnlag = 1000, refusjonsbeløp = 0)))
        val første = opprettUtbetaling(tidslinje.kutt(21.januar))
        val andre = opprettUtbetaling(tidslinje, første)
        assertEquals(første.inspektør.personOppdrag.fagsystemId(), andre.inspektør.personOppdrag.fagsystemId())
        assertEquals(første.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), andre.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
        assertEquals(første.inspektør.korrelasjonsId, andre.inspektør.korrelasjonsId)
    }

    @Test
    fun `korrelasjonsId er lik på brukerutbetalinger fra Infotrygd`() {
        val tidslinje = beregnUtbetalinger(
            tidslinjeOf(16.AP, 15.NAV(dekningsgrunnlag = 1000, refusjonsbeløp = 0), 5.UKJ, 23.NAV(dekningsgrunnlag = 1000, refusjonsbeløp = 0))
        )
        val første = opprettUtbetaling(tidslinje.kutt(31.januar))
        val andre = opprettUtbetaling(tidslinje.kutt(20.februar), første)
        val tredje = opprettUtbetaling(tidslinje, andre)
        assertNotEquals(første.inspektør.korrelasjonsId, andre.inspektør.korrelasjonsId)
        assertEquals(andre.inspektør.personOppdrag.fagsystemId(), tredje.inspektør.personOppdrag.fagsystemId())
        assertEquals(andre.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), tredje.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
        assertEquals(andre.inspektør.korrelasjonsId, tredje.inspektør.korrelasjonsId)
    }

    @Test
    fun `korrelasjonsId er lik på arbeidsgiveroppdrag direkte fra Infotrygd`() {
        val tidslinje = beregnUtbetalinger(tidslinjeOf(5.UKJ, 26.NAV))
        val første = opprettUtbetaling(tidslinje.kutt(21.januar))
        val andre = opprettUtbetaling(tidslinje, første)
        assertEquals(første.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), andre.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
        assertEquals(første.inspektør.personOppdrag.fagsystemId(), andre.inspektør.personOppdrag.fagsystemId())
        assertEquals(første.inspektør.korrelasjonsId, andre.inspektør.korrelasjonsId)
    }

    @Test
    fun `korrelasjonsId er lik på arbeidsgiveroppdrag fra Infotrygd`() {
        val tidslinje = beregnUtbetalinger(tidslinjeOf(16.AP, 15.NAV, 5.UKJ, 23.NAV))
        val første = opprettUtbetaling(tidslinje.kutt(31.januar))
        val andre = opprettUtbetaling(tidslinje.kutt(20.februar), første)
        val tredje = opprettUtbetaling(tidslinje, andre)
        assertNotEquals(første.inspektør.korrelasjonsId, andre.inspektør.korrelasjonsId)
        assertEquals(andre.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), tredje.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
        assertEquals(andre.inspektør.personOppdrag.fagsystemId(), tredje.inspektør.personOppdrag.fagsystemId())
        assertEquals(andre.inspektør.korrelasjonsId, tredje.inspektør.korrelasjonsId)
    }

    @Test
    fun `overføre utbetaling med delvis refusjon`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV(dekningsgrunnlag = 1000, refusjonsbeløp = 600))
        beregnUtbetalinger(tidslinje)
        val utbetaling = opprettUbetaltUtbetaling(tidslinje)
        val hendelselogg = godkjenn(utbetaling)
        val utbetalingsbehov = hendelselogg.behov().filter { it.type == Behovtype.Utbetaling }
        assertEquals(2, utbetalingsbehov.size) { "Forventer to utbetalingsbehov" }
        val fagområder = utbetalingsbehov.map { it.detaljer().getValue("fagområde") as String }
        assertTrue(Fagområde.Sykepenger.verdi in fagområder)
        assertTrue(Fagområde.SykepengerRefusjon.verdi in fagområder)
    }

    @Test
    fun `overføre utbetaling med null refusjon`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV(dekningsgrunnlag = 1000, refusjonsbeløp = 0))
        beregnUtbetalinger(tidslinje)
        val utbetaling = opprettUbetaltUtbetaling(tidslinje)
        val hendelselogg = godkjenn(utbetaling)
        val utbetalingsbehov = hendelselogg.behov().filter { it.type == Behovtype.Utbetaling }
        assertEquals(1, utbetalingsbehov.size) { "Forventer bare ett utbetalingsbehov" }
        assertEquals(Fagområde.Sykepenger.verdi, utbetalingsbehov.first().detaljer().getValue("fagområde"))
    }

    @Test
    fun `tre utbetalinger`() {
        val tidslinje = tidslinjeOf(
            16.AP, 3.NAV, 5.NAV(1200, 50.0), 7.NAV,
            startDato = 1.januar(2020)
        )

        beregnUtbetalinger(tidslinje)

        val første = opprettUtbetaling(tidslinje.kutt(19.januar(2020)))
        val andre = opprettUtbetaling(tidslinje.kutt(24.januar(2020)), tidligere = første)
        val tredje = opprettUtbetaling(tidslinje.kutt(31.januar(2020)), tidligere = andre)

        val inspektør = tredje.inspektør.arbeidsgiverOppdrag.inspektør
        assertEquals(3, inspektør.antallLinjer())
        assertNull(inspektør.refDelytelseId(0))
        assertNull(inspektør.refFagsystemId(0))

        assertEquals(1, inspektør.delytelseId(0))
        assertEquals(2, inspektør.delytelseId(1))
        assertEquals(3, inspektør.delytelseId(2))

        assertEquals(inspektør.delytelseId(0), inspektør.refDelytelseId(1))
        assertEquals(inspektør.delytelseId(1), inspektør.refDelytelseId(2))

        assertEquals(første.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), inspektør.refFagsystemId(1))
        assertEquals(andre.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), inspektør.refFagsystemId(2))
    }

    @Test
    fun `overgang fra full til null refusjon`() {
        val tidslinje = tidslinjeOf(
            16.AP,
            17.NAV,
            5.NAV(1200, refusjonsbeløp = 0.0),
            9.NAV,
            1.ARB,
            4.NAV(1200, refusjonsbeløp = 0.0)
        )

        beregnUtbetalinger(tidslinje)

        val første = opprettUtbetaling(tidslinje.kutt(26.januar))
        val andre = opprettUtbetaling(tidslinje.kutt(31.januar), tidligere = første)
        val tredje = opprettUtbetaling(tidslinje.kutt(7.februar), tidligere = andre)
        val fjerde = opprettUtbetaling(tidslinje.kutt(14.februar), tidligere = tredje)
        val femte = opprettUtbetaling(tidslinje.kutt(21.februar), tidligere = fjerde)

        assertEquals(første.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), andre.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
        assertEquals(første.inspektør.korrelasjonsId, andre.inspektør.korrelasjonsId)
        assertEquals(andre.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), tredje.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
        assertEquals(andre.inspektør.korrelasjonsId, tredje.inspektør.korrelasjonsId)
        assertEquals(tredje.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), fjerde.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
        assertEquals(tredje.inspektør.korrelasjonsId, fjerde.inspektør.korrelasjonsId)
        assertEquals(fjerde.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), femte.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId())
        assertEquals(fjerde.inspektør.korrelasjonsId, femte.inspektør.korrelasjonsId)

        assertNotEquals(første.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), første.inspektør.personOppdrag.fagsystemId())

        assertEquals(første.inspektør.personOppdrag.fagsystemId(), andre.inspektør.personOppdrag.fagsystemId())
        assertEquals(andre.inspektør.personOppdrag.fagsystemId(), tredje.inspektør.personOppdrag.fagsystemId())
        assertEquals(tredje.inspektør.personOppdrag.fagsystemId(), fjerde.inspektør.personOppdrag.fagsystemId())
        assertEquals(fjerde.inspektør.personOppdrag.fagsystemId(), femte.inspektør.personOppdrag.fagsystemId())

        assertEquals(0, første.inspektør.personOppdrag.inspektør.antallLinjer())
        assertEquals(0, andre.inspektør.personOppdrag.inspektør.antallLinjer())
        assertEquals(1, tredje.inspektør.personOppdrag.inspektør.antallLinjer())
        assertEquals(1, fjerde.inspektør.personOppdrag.inspektør.antallLinjer())
        assertEquals(2, femte.inspektør.personOppdrag.inspektør.antallLinjer())
    }

    @Test
    fun `utbetalingOverført som ikke treffer på fagsystemId`() {
        val utbetaling = opprettGodkjentUtbetaling()
        overfør(utbetaling, "feil fagsystemId")
        assertEquals(Utbetaling.Sendt, utbetaling.inspektør.tilstand)
    }

    @Test
    fun `utbetalingOverført som treffer på arbeidsgiverFagsystemId`() {
        val utbetaling = opprettGodkjentUtbetaling()
        overfør(utbetaling)
        assertEquals(Utbetaling.Overført, utbetaling.inspektør.tilstand)
    }

    @Test
    fun `utbetalingOverført som treffer på brukerFagsystemId`() {
        val utbetaling = opprettGodkjentUtbetaling()
        overfør(utbetaling, utbetaling.inspektør.personOppdrag.fagsystemId())
        assertEquals(Utbetaling.Overført, utbetaling.inspektør.tilstand)
    }

    @Test
    fun `utbetalingOverført som bommer på utbetalingId`() {
        val utbetaling = opprettGodkjentUtbetaling()
        overfør(utbetaling, utbetaling.inspektør.personOppdrag.fagsystemId(), UUID.randomUUID())
        assertEquals(Utbetaling.Sendt, utbetaling.inspektør.tilstand)
    }

    @Test
    fun `utbetalingHendelse som treffer på brukeroppdraget`() {
        val utbetaling = opprettGodkjentUtbetaling()
        overfør(utbetaling, utbetaling.inspektør.personOppdrag.fagsystemId(), UUID.randomUUID())
        kvittèr(utbetaling, utbetaling.inspektør.personOppdrag.fagsystemId())
        assertEquals(Utbetaling.Utbetalt, utbetaling.inspektør.tilstand)
    }

    @Test
    fun `g-regulering skal treffe personOppdrag`() {
        val utbetalingMedPersonOppdragMatch = opprettGodkjentUtbetaling()
        val personFagsystemId = utbetalingMedPersonOppdragMatch.inspektør.personOppdrag.fagsystemId()
        val arbeidsgiverFagsystemId = utbetalingMedPersonOppdragMatch.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId()
        val utbetalingUtenMatch = opprettGodkjentUtbetaling()
        val utbetalinger = listOf(utbetalingUtenMatch, utbetalingMedPersonOppdragMatch)

        val funnetArbeidsgiverUtbetaling = Utbetaling.finnUtbetalingForJustering(utbetalinger, arbeidsgiverFagsystemId.gRegulering())
        assertEquals(utbetalingMedPersonOppdragMatch, funnetArbeidsgiverUtbetaling, "Fant ikke arbeidsgiverutbetaling")

        val funnetUtbetaling = Utbetaling.finnUtbetalingForJustering(utbetalinger, personFagsystemId.gRegulering())
        assertEquals(utbetalingMedPersonOppdragMatch, funnetUtbetaling, "Fant ikke personutbetaling")

        assertNull(Utbetaling.finnUtbetalingForJustering(utbetalinger, "somethingrandom".gRegulering()))
    }

    @Test
    fun `serialiserer avstemmingsnøkkel som null når den ikke er satt`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV(dekningsgrunnlag = 1000))
        beregnUtbetalinger(tidslinje)
        val utbetaling = opprettUbetaltUtbetaling(tidslinje)
        assertNull(utbetaling.inspektør.avstemmingsnøkkel)
    }

    @Test
    fun `simulering som er relevant for personoppdrag`() {
        val utbetalingId = UUID.randomUUID()
        val simulering = opprettSimulering("1", Fagområde.Sykepenger, utbetalingId)
        assertTrue(simulering.erRelevantForUtbetaling(utbetalingId))
        assertFalse(simulering.erRelevantForUtbetaling(UUID.randomUUID()))
        assertTrue(simulering.erRelevantFor(Fagområde.Sykepenger, "1"))
        assertFalse(simulering.erRelevantFor(Fagområde.Sykepenger, "2"))
        assertFalse(simulering.erRelevantFor(Fagområde.SykepengerRefusjon, "1"))
    }

    @Test
    fun `simulering som er relevant for arbeidsgiveroppdrag`() {
        val utbetalingId = UUID.randomUUID()
        val simulering = opprettSimulering("1", Fagområde.SykepengerRefusjon, utbetalingId)
        assertTrue(simulering.erRelevantForUtbetaling(utbetalingId))
        assertFalse(simulering.erRelevantForUtbetaling(UUID.randomUUID()))
        assertTrue(simulering.erRelevantFor(Fagområde.SykepengerRefusjon, "1"))
        assertFalse(simulering.erRelevantFor(Fagområde.SykepengerRefusjon, "2"))
        assertFalse(simulering.erRelevantFor(Fagområde.Sykepenger, "1"))
    }

    @Test
    fun `simulerer ingen refusjon`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV(dekningsgrunnlag = 1000, refusjonsbeløp = 0))
        beregnUtbetalinger(tidslinje)
        val utbetaling = opprettUbetaltUtbetaling(tidslinje)
        val simulering = opprettSimulering(
            utbetaling.inspektør.personOppdrag.fagsystemId(), Fagområde.Sykepenger, utbetaling.inspektør.utbetalingId, SimuleringResultat(
                totalbeløp = 1000,
                perioder = emptyList()
            )
        )
        utbetaling.håndter(simulering)
        assertNotNull(utbetaling.inspektør.personOppdrag.inspektør.simuleringsResultat())
        assertNull(utbetaling.inspektør.arbeidsgiverOppdrag.inspektør.simuleringsResultat())
    }

    @Test
    fun `simulerer full refusjon`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV(dekningsgrunnlag = 1000, refusjonsbeløp = 1000))
        beregnUtbetalinger(tidslinje)
        val utbetaling = opprettUbetaltUtbetaling(tidslinje)
        val simulering = opprettSimulering(
            utbetaling.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), Fagområde.SykepengerRefusjon, utbetaling.inspektør.utbetalingId, SimuleringResultat(
                totalbeløp = 1000,
                perioder = emptyList()
            )
        )
        utbetaling.håndter(simulering)
        assertNotNull(utbetaling.inspektør.arbeidsgiverOppdrag.inspektør.simuleringsResultat())
        assertNull(utbetaling.inspektør.personOppdrag.inspektør.simuleringsResultat())
    }

    @Test
    fun `simulerer delvis refusjon`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV(dekningsgrunnlag = 1000, refusjonsbeløp = 500))
        beregnUtbetalinger(tidslinje)
        val utbetaling = opprettUbetaltUtbetaling(tidslinje)

        val simuleringArbeidsgiver = opprettSimulering(
            utbetaling.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), Fagområde.SykepengerRefusjon, utbetaling.inspektør.utbetalingId, SimuleringResultat(
                totalbeløp = 500,
                perioder = emptyList()
            )
        )
        val simuleringPerson = opprettSimulering(
            utbetaling.inspektør.personOppdrag.fagsystemId(), Fagområde.Sykepenger, utbetaling.inspektør.utbetalingId, SimuleringResultat(
                totalbeløp = 500,
                perioder = emptyList()
            )
        )
        utbetaling.håndter(simuleringArbeidsgiver)
        utbetaling.håndter(simuleringPerson)

        assertNotNull(utbetaling.inspektør.arbeidsgiverOppdrag.inspektør.simuleringsResultat())
        assertNotNull(utbetaling.inspektør.personOppdrag.inspektør.simuleringsResultat())
    }

    private fun opprettSimulering(fagsystemId: String, fagområde: Fagområde, utbetalingId: UUID, simuleringResultat: SimuleringResultat? = null) =
        Simulering(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = "1",
            aktørId = "aktørId",
            fødselsnummer = "fødselsnummer",
            orgnummer = "orgnummer",
            fagsystemId = fagsystemId,
            fagområde = fagområde.verdi,
            simuleringOK = true,
            melding = "melding",
            simuleringResultat = simuleringResultat,
            utbetalingId = utbetalingId
        )

    private fun String.gRegulering() = Grunnbeløpsregulering(UUID.randomUUID(), "", "", "", LocalDate.now(), this)

    private fun beregnUtbetalinger(tidslinje: Utbetalingstidslinje) = tidslinje.also { MaksimumUtbetalingFilter().betal(listOf(tidslinje), tidslinje.periode(), aktivitetslogg, MaskinellJurist()) }

    private fun opprettGodkjentUtbetaling(
        tidslinje: Utbetalingstidslinje = tidslinjeOf(16.AP, 5.NAV(3000)),
        sisteDato: LocalDate = tidslinje.periode().endInclusive,
        fødselsnummer: String = UNG_PERSON_FNR_2018,
        orgnummer: String = ORGNUMMER,
        aktivitetslogg: Aktivitetslogg = this.aktivitetslogg
    ) = opprettUbetaltUtbetaling(beregnUtbetalinger(tidslinje), null, sisteDato, fødselsnummer, orgnummer, aktivitetslogg)
        .also { godkjenn(it) }

    private fun opprettUbetaltUtbetaling(
        tidslinje: Utbetalingstidslinje,
        tidligere: Utbetaling? = null,
        sisteDato: LocalDate = tidslinje.periode().endInclusive,
        fødselsnummer: String = UNG_PERSON_FNR_2018,
        orgnummer: String = ORGNUMMER,
        aktivitetslogg: Aktivitetslogg = this.aktivitetslogg
    ) = Utbetaling.lagUtbetaling(
        tidligere?.let { listOf(tidligere) } ?: emptyList(),
        fødselsnummer,
        UUID.randomUUID(),
        orgnummer,
        tidslinje,
        sisteDato,
        aktivitetslogg,
        LocalDate.MAX,
        100,
        148
    ).also { it.opprett(aktivitetslogg) }

    private fun opprettUtbetaling(
        tidslinje: Utbetalingstidslinje,
        tidligere: Utbetaling? = null,
        sisteDato: LocalDate = tidslinje.periode().endInclusive,
        fødselsnummer: String = UNG_PERSON_FNR_2018,
        orgnummer: String = ORGNUMMER,
        aktivitetslogg: Aktivitetslogg = this.aktivitetslogg
    ) = opprettUbetaltUtbetaling(tidslinje, tidligere, sisteDato, fødselsnummer, orgnummer, aktivitetslogg).also { utbetaling ->
        godkjenn(utbetaling)
        listOf(utbetaling.inspektør.arbeidsgiverOppdrag, utbetaling.inspektør.personOppdrag)
            .filter { it.harUtbetalinger() }
            .map { it.fagsystemId() }
            .onEach { overfør(utbetaling, it) }
            .onEach { kvittèr(utbetaling, it) }
    }

    private fun overfør(
        utbetaling: Utbetaling,
        fagsystemId: String = utbetaling.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(),
        utbetalingId: UUID = utbetaling.inspektør.utbetalingId
    ) {
        utbetaling.håndter(
            UtbetalingOverført(
                meldingsreferanseId = UUID.randomUUID(),
                aktørId = "ignore",
                fødselsnummer = "ignore",
                orgnummer = "ignore",
                fagsystemId = fagsystemId,
                utbetalingId = "$utbetalingId",
                avstemmingsnøkkel = 123456L,
                overføringstidspunkt = LocalDateTime.now()
            )
        )
    }

    private fun kvittèr(
        utbetaling: Utbetaling,
        fagsystemId: String = utbetaling.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(),
        status: Oppdragstatus = AKSEPTERT
    ) {
        utbetaling.håndter(
            UtbetalingHendelse(
                meldingsreferanseId = UUID.randomUUID(),
                aktørId = "ignore",
                fødselsnummer = UNG_PERSON_FNR_2018,
                orgnummer = ORGNUMMER,
                fagsystemId = fagsystemId,
                utbetalingId = "${utbetaling.inspektør.utbetalingId}",
                status = status,
                melding = "hei",
                avstemmingsnøkkel = 123456L,
                overføringstidspunkt = LocalDateTime.now()
            )
        )
    }

    private fun godkjenn(utbetaling: Utbetaling, utbetalingGodkjent: Boolean = true) =
        Utbetalingsgodkjenning(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = "ignore",
            fødselsnummer = "ignore",
            organisasjonsnummer = "ignore",
            utbetalingId = utbetaling.inspektør.utbetalingId,
            vedtaksperiodeId = "ignore",
            saksbehandler = "Z999999",
            saksbehandlerEpost = "mille.mellomleder@nav.no",
            utbetalingGodkjent = utbetalingGodkjent,
            godkjenttidspunkt = LocalDateTime.now(),
            automatiskBehandling = false,
        ).also {
            utbetaling.håndter(it)
        }

    private fun annuller(utbetaling: Utbetaling, fagsystemId: String = utbetaling.inspektør.arbeidsgiverOppdrag.inspektør.fagsystemId()) =
        utbetaling.annuller(AnnullerUtbetaling(UUID.randomUUID(), "aktør", "fnr", "orgnr", fagsystemId, "Z123456", "tbd@nav.no", LocalDateTime.now()))?.also {
            it.opprett(aktivitetslogg)
        }
}
