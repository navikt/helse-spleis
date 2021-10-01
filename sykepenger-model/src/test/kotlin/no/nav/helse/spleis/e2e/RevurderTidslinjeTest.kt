package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.person.*
import no.nav.helse.person.IdInnhenter
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import java.time.LocalDateTime

@TestInstance(Lifecycle.PER_CLASS)
internal class RevurderTidslinjeTest : AbstractEndToEndTest() {

    @Test
    fun `to perioder - revurder dager i eldste`() {
        nyttVedtak(3.januar, 26.januar)
        forlengVedtak(27.januar, 14.februar)

        håndterOverstyring((20.januar til 22.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode)

        assertNoErrors(inspektør)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVSLUTTET
        )

        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_ARBEIDSGIVERE_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `revurdering blør ikke ned i sykdomstidslinja på vedtaksperiodenivå`() {
        nyttVedtak(3.januar, 26.januar)
        forlengVedtak(27.januar, 14.februar)

        håndterOverstyring((20.januar til 22.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode)

        assertNoErrors(inspektør)
        assertEquals(3, inspektør.sykdomshistorikkDagTeller[Dag.Feriedag::class])
        assertNull(inspektør.vedtaksperiodeDagTeller(1.vedtaksperiode)?.get(Dag.Feriedag::class))
        assertNull(inspektør.vedtaksperiodeDagTeller(2.vedtaksperiode)?.get(Dag.Feriedag::class))
    }

    @Test
    fun `to perioder - revurder dag i nyeste`() {
        nyttVedtak(3.januar, 26.januar)
        forlengVedtak(27.januar, 14.februar)

        håndterOverstyring((4.februar til 8.februar).map { manuellFeriedag(it) })
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode)

        assertNoErrors(inspektør)
        assertTilstander(
            0,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
        )

        assertTilstander(
            1,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            AVSLUTTET,
        )

        assertEquals(3, inspektør.utbetalinger.size)
        assertEquals(6, inspektør.forbrukteSykedager(0))
        assertEquals(19, inspektør.forbrukteSykedager(1))
        assertEquals(15, inspektør.forbrukteSykedager(2))
    }

    @Test
    @Disabled
    fun `to perioder - hele den eldste perioden blir ferie`() {
        nyttVedtak(3.januar, 26.januar)
        forlengVedtak(27.januar, 14.februar)

        håndterOverstyring((3.januar til 26.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode)

        assertTilstander(
            0,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_HISTORIKK,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVSLUTTET
        )

        assertTilstander(
            1,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_ARBEIDSGIVERE_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            AVSLUTTET,
        )

        assertEquals(3, inspektør.utbetalinger.size)
        assertEquals(6, inspektør.forbrukteSykedager(0))
        assertEquals(19, inspektør.forbrukteSykedager(1))
        assertEquals(6, inspektør.forbrukteSykedager(2))

    }

    @Test
    fun `to perioder - hele den nyeste perioden blir ferie`() {
        nyttVedtak(3.januar, 26.januar)
        forlengVedtak(27.januar, 14.februar)

        håndterOverstyring((27.januar til 14.februar).map { manuellFeriedag(it) })
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode)

        assertNoErrors(inspektør)

        assertTilstander(
            0,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
        )

        assertTilstander(
            1,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            AVSLUTTET,
        )

        assertEquals(3, inspektør.utbetalinger.size)
        assertEquals(6, inspektør.forbrukteSykedager(0))
        assertEquals(19, inspektør.forbrukteSykedager(1))
        assertEquals(6, inspektør.forbrukteSykedager(2))
    }

    @Test
    fun `to perioder - hele den eldste perioden blir sykedager`() {
    } //???? Skal hele den først perioden være en søknad med bare ferie..?

    @Test
    fun `kan ikke utvide perioden med sykedager`() {
        nyttVedtak(3.januar, 26.januar)
        håndterOverstyring((25.januar til 14.februar).map { manuellSykedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)

        assertTilstander(
            0,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            AVSLUTTET
        )

        assertEquals(Utbetaling.GodkjentUtenUtbetaling, inspektør.utbetalingtilstand(1))
    }

    @Test
    @Disabled
    fun `forsøk på å flytte arbeidsgiverperioden`() {
    } // Hvordan gjør man det?

    @Test
    fun `ferie i arbeidsgiverperiode`() {
        nyttVedtak(3.januar, 26.januar)
        håndterSykmelding(Sykmeldingsperiode(27.januar, 14.februar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(27.januar, 14.februar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        håndterOverstyring((6.januar til 9.januar).map { manuellFeriedag(it) })  // ferie på første navdag
        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            AVSLUTTET
        )

        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            AVVENTER_UFERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK
        )

        assertNoErrors(inspektør)
        assertEquals(3, inspektør.utbetalinger.size)
        assertFalse(inspektør.utbetaling(2).harUtbetalinger())

    }

    @Test
    fun `ledende uferdig periode`() {
        nyttVedtak(3.januar, 26.januar)
        håndterSykmelding(Sykmeldingsperiode(27.januar, 14.februar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(27.januar, 14.februar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        håndterOverstyring((19.januar til 22.januar).map { manuellFeriedag(it) })  // ferie på første navdag
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            AVVENTER_UFERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK
        )
        val revurdering = inspektør.utbetaling(2)
        assertNoErrors(inspektør)
        assertEquals(2, revurdering.arbeidsgiverOppdrag().size)
        assertEquals(19.januar, revurdering.arbeidsgiverOppdrag()[0].datoStatusFom())
        assertEquals(23.januar til 26.januar, revurdering.arbeidsgiverOppdrag()[1].periode)
    }

    @Test
    fun `ledende uferdig periode som ikke har en utbetaling`() {
        nyttVedtak(3.januar, 26.januar)
        håndterSykmelding(Sykmeldingsperiode(27.januar, 14.februar, 100.prosent))

        håndterOverstyring((6.januar til 10.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            AVSLUTTET
        )

        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE
        )
        assertNoErrors(inspektør)
    }

    @Test
    fun `forsøk på å revurdere eldre fagsystemId`() {
        nyttVedtak(3.januar, 26.januar)
        nyttVedtak(3.mars, 26.mars)
        håndterOverstyring((4.januar til 20.januar).map { manuellFeriedag(it) })
        SykdomstidslinjeInspektør(inspektør.sykdomstidslinje).also { sykdomstidslinjeInspektør ->
            assertTrue((4.januar til 20.januar).none { sykdomstidslinjeInspektør.dager[it] == Dag.Feriedag::class })
        }
        assertTrue(hendelselogg.hasErrorsOrWorse())
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `forsøk på å revurdere eldre fagsystemId med nyere periode til godkjenning`() {
        nyttVedtak(3.januar, 26.januar)
        tilGodkjenning(3.mars, 26.mars, 100.prosent, 3.mars)
        håndterOverstyring((4.januar til 20.januar).map { manuellFeriedag(it) })
        SykdomstidslinjeInspektør(inspektør.sykdomstidslinje).also { sykdomstidslinjeInspektør ->
            assertTrue((4.januar til 20.januar).none { sykdomstidslinjeInspektør.dager[it] == Dag.Feriedag::class })
        }
        assertTrue(hendelselogg.hasErrorsOrWorse())
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING
        )
    }

    @Test
    fun `samme fagsystemId og forskjellig skjæringstidspunkt - revurder første periode i siste skjæringstidspunkt`() {
        nyttVedtak(3.januar, 26.januar)
        nyttVedtak(1.februar, 20.februar)
        forlengVedtak(21.februar, 10.mars)

        håndterOverstyring((5.februar til 15.februar).map { manuellFeriedag(it) })
        håndterYtelser(2.vedtaksperiode)
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt(3.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVSLUTTET
        )
        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_ARBEIDSGIVERE_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `samme fagsystemId og forskjellig skjæringstidspunkt - revurder siste periode i siste skjæringstidspunkt`() {
        nyttVedtak(3.januar, 26.januar)
        nyttVedtak(1.februar, 20.februar)
        forlengVedtak(21.februar, 10.mars)

        håndterOverstyring((22.februar til 25.februar).map { manuellFeriedag(it) })
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt(3.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }


    @Test
    fun `forsøk på å revurdere eldre fagsystemId med nyere perioder uten utbetaling, og periode med utbetaling etterpå`() {
        nyttVedtak(3.januar, 26.januar)
        tilGodkjenning(1.mai, 31.mai, 100.prosent, 1.mai)
        håndterSykmelding(Sykmeldingsperiode(3.mars, 15.mars, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(16.mars, 26.mars, 100.prosent))

        håndterOverstyring((4.januar til 20.januar).map { manuellFeriedag(it) })
        SykdomstidslinjeInspektør(inspektør.sykdomstidslinje).also { sykdomstidslinjeInspektør ->
            assertTrue((4.januar til 20.januar).none { sykdomstidslinjeInspektør.dager[it] == Dag.Feriedag::class })
        }
        assertTrue(hendelselogg.hasErrorsOrWorse())
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            AVVENTER_UFERDIG_GAP
        )
        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            MOTTATT_SYKMELDING_FERDIG_GAP
        )
        assertTilstander(
            4.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE
        )
    }

    @Test
    fun `forsøk på å revurdere eldre fagsystemId med nyere perioder med utbetaling, og periode uten utbetaling`() {
        nyttVedtak(3.januar, 26.januar)
        nyttVedtak(1.mai, 31.mai)
        håndterSykmelding(Sykmeldingsperiode(1.juni, 14.juni, 100.prosent))

        håndterOverstyring((4.januar til 20.januar).map { manuellFeriedag(it) })
        SykdomstidslinjeInspektør(inspektør.sykdomstidslinje).also { sykdomstidslinjeInspektør ->
            assertTrue((4.januar til 20.januar).none { sykdomstidslinjeInspektør.dager[it] == Dag.Feriedag::class })
        }
        assertTrue(hendelselogg.hasErrorsOrWorse())
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE
        )
    }

    @Test
    fun `revurderer siste utbetalte periode med bare ferie og permisjon`() {
        nyttVedtak(3.januar, 26.januar)

        håndterOverstyring((3.januar til 20.januar).map { manuellFeriedag(it) } + (21.januar til 26.januar).map { manuellPermisjonsdag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode)

        assertTilstander(
            0,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            AVSLUTTET,
        )

        assertNoErrors(inspektør)
        assertEquals(2, inspektør.utbetalinger.size)
        assertEquals(6, inspektør.forbrukteSykedager(0))
        assertEquals(0, inspektør.forbrukteSykedager(1))
    }

    @Test
    fun `Avslag fører til feilet revurdering`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 17.januar)), førsteFraværsdag = 1.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 31.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode, besvart = LocalDateTime.now().minusHours(24))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode, besvart = LocalDateTime.now().minusHours(24))
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode)

        håndterOverstyring(listOf(manuellFeriedag(18.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, utbetalingGodkjent = false)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            REVURDERING_FEILET
        )
        assertWarn("Utbetaling av revurdert periode ble avvist av saksbehandler. Utbetalingen må annulleres", inspektør.personLogg)
    }

    @Test
    fun `Feilet simulering fører til feilet revurdering`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 17.januar)), førsteFraværsdag = 1.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 31.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode, besvart = LocalDateTime.now().minusHours(24))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode, besvart = LocalDateTime.now().minusHours(24))
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode)

        håndterOverstyring(listOf(manuellFeriedag(18.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode, simuleringOK = false)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            REVURDERING_FEILET
        )
        assertWarn("Simulering av revurdert utbetaling feilet. Utbetalingen må annulleres", inspektør.personLogg)
    }

    @Test
    fun `annullering av feilet revurdering`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 17.januar)), førsteFraværsdag = 1.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 31.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode)

        håndterOverstyring(listOf(manuellFeriedag(18.januar)))
        håndterYtelser(
            1.vedtaksperiode,
            foreldrepenger = 16.januar til 28.januar
        )

        håndterAnnullerUtbetaling()

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_HISTORIKK_REVURDERING,
            REVURDERING_FEILET
        )
    }

    @Test
    fun `påminnet revurdering timer ikke ut`() {
        nyttVedtak(3.januar, 26.januar)

        håndterOverstyring((16.januar til 26.januar).map { manuellFeriedag(it) })

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterPåminnelse(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING, LocalDateTime.now().minusDays(14))

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING
        )
    }

    @Test
    fun `revurder med kun ferie`() {
        nyttVedtak(3.januar, 26.januar)
        forlengVedtak(27.januar, 13.februar)
        forlengPeriode(14.februar, 15.februar)

        håndterOverstyring((27.januar til 13.februar).map { manuellFeriedag(it) })
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(2.vedtaksperiode)

        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_UFERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK
        )
        assertNoErrors(inspektør)
    }

    @Test
    fun `revurder en revurdering`() {
        nyttVedtak(3.januar, 26.januar)

        håndterOverstyring((20.januar til 22.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode)

        håndterOverstyring((23.januar til 23.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        assertNoErrors(inspektør)
        assertEquals(6, inspektør.forbrukteSykedager(0))
        assertEquals(5, inspektør.forbrukteSykedager(1))
        assertEquals(4, inspektør.forbrukteSykedager(2))
    }

    @Test
    fun `tre utbetalte perioder - midterste blir revurdert - to siste perioder blir revurdert, første er urørt`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)

        håndterOverstyring((15.februar til 28.februar).map { manuellFeriedag(it) })

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_HISTORIKK_REVURDERING
        )

        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_ARBEIDSGIVERE_REVURDERING
        )
        assertNoErrors(inspektør)
    }

    @Test
    fun `tre utbetalte perioder - midterste blir revurdert og utbetalt - to siste perioder blir revurdert, første er urørt`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)

        håndterOverstyring((15.februar til 28.februar).map { manuellFeriedag(it) })
        håndterYtelser(2.vedtaksperiode)
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt(3.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVSLUTTET
        )

        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_ARBEIDSGIVERE_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertNoErrors(inspektør)
        assertEquals(0, inspektør.ikkeUtbetalteUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
        assertEquals(0, inspektør.ikkeUtbetalteUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
        assertEquals(0, inspektør.ikkeUtbetalteUtbetalingerForVedtaksperiode(3.vedtaksperiode).size)
        assertEquals(1, inspektør.avsluttedeUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
        assertEquals(2, inspektør.avsluttedeUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
        assertEquals(2, inspektør.avsluttedeUtbetalingerForVedtaksperiode(3.vedtaksperiode).size)
    }

    @Test
    fun `forleng uferdig revurdering`() {
        nyttVedtak(3.januar, 26.januar)

        håndterOverstyring((16.januar til 26.januar).map { manuellFeriedag(it) })

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(27.januar, 5.februar, 100.prosent))
        håndterSøknad(Sykdom(27.januar, 5.februar, 100.prosent))

        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)

        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `forleng uferdig revurdering med gap`() {
        nyttVedtak(3.januar, 26.januar)

        håndterOverstyring((16.januar til 26.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(30.januar, 5.februar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 30.januar)
        håndterSøknad(Sykdom(30.januar, 5.februar, 100.prosent))

        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)

        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            AVVENTER_SØKNAD_UFERDIG_GAP,
            AVVENTER_UFERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `feilet revurdering blokkerer videre behandling`() {
        nyttVedtak(3.januar, 26.januar)

        håndterOverstyring((16.januar til 26.januar).map { manuellFeriedag(it) })

        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, utbetalingGodkjent = false)

        håndterSykmelding(Sykmeldingsperiode(27.januar, 5.februar, 100.prosent))
        håndterSøknad(Sykdom(27.januar, 5.februar, 100.prosent))

        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE
        )
    }

    @Test
    fun `feilet revurdering prodsak`() {
        /* Vi ser flere innslag hvor revurdering har feilet og havnet i REVURDERING_FEILET. Det ser ut som ferie i etterfølgende periode er årsaken.
        * Her er en feilende test. Værsågod :)
        * */
        nyttVedtak(3.januar, 26.januar)

        håndterSykmelding(Sykmeldingsperiode(2.februar, 27.februar, 100.prosent))
        håndterSøknad(Sykdom(2.februar, 27.februar, 100.prosent), Søknad.Søknadsperiode.Ferie(2.februar, 20.februar))

        håndterOverstyring((25.januar til 26.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
        )
    }

    @Test
    fun `overstyre periode som etterfølges av forkastede perioder`() {
        // Fremprovoserer et case hvor det at inntektsmeldingen kaster ut etterfølgende perioder fjerner låsene i sykdomshistorikken. Da kræsjer revurderingen
        // fordi vi forsøker å låse opp perioder som ikke er låste.

        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)
        håndterSykmelding(Sykmeldingsperiode(1.april, 10.april, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), refusjon = Refusjon(2.april, INNTEKT))

        håndterOverstyring((1..31).map { ManuellOverskrivingDag(it.mars, Dagtype.Feriedag) })

        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_HISTORIKK_REVURDERING
        )
    }

    private fun assertEtterspurteYtelser(expected: Int, vedtaksperiodeIdInnhenter: IdInnhenter) {
        assertEquals(expected, inspektør.antallEtterspurteBehov(vedtaksperiodeIdInnhenter, Aktivitetslogg.Aktivitet.Behov.Behovtype.Foreldrepenger))
        assertEquals(expected, inspektør.antallEtterspurteBehov(vedtaksperiodeIdInnhenter, Aktivitetslogg.Aktivitet.Behov.Behovtype.Pleiepenger))
        assertEquals(expected, inspektør.antallEtterspurteBehov(vedtaksperiodeIdInnhenter, Aktivitetslogg.Aktivitet.Behov.Behovtype.Omsorgspenger))
        assertEquals(expected, inspektør.antallEtterspurteBehov(vedtaksperiodeIdInnhenter, Aktivitetslogg.Aktivitet.Behov.Behovtype.Opplæringspenger))
        assertEquals(expected, inspektør.antallEtterspurteBehov(vedtaksperiodeIdInnhenter, Aktivitetslogg.Aktivitet.Behov.Behovtype.Arbeidsavklaringspenger))
        assertEquals(expected, inspektør.antallEtterspurteBehov(vedtaksperiodeIdInnhenter, Aktivitetslogg.Aktivitet.Behov.Behovtype.Dagpenger))
        assertEquals(expected, inspektør.antallEtterspurteBehov(vedtaksperiodeIdInnhenter, Aktivitetslogg.Aktivitet.Behov.Behovtype.Institusjonsopphold))
        assertEquals(expected, inspektør.antallEtterspurteBehov(vedtaksperiodeIdInnhenter, Aktivitetslogg.Aktivitet.Behov.Behovtype.Dødsinfo))
    }

    @Test
    fun `etterspør ytelser ved påminnelser i avventer_historikk_revurdering`() {
        nyttVedtak(1.januar, 31.januar)
        assertEtterspurteYtelser(2, 1.vedtaksperiode)

        håndterOverstyring((25.januar til 26.januar).map { manuellFeriedag(it) })
        assertEtterspurteYtelser(3, 1.vedtaksperiode)

        håndterPåminnelse(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        assertEtterspurteYtelser(4, 1.vedtaksperiode)
    }

    @Test
    @Disabled
    fun `BUG Overstyring av utkast til revurdering der en av periodene som revurderes er i REVURDERING_FEILET fører til at vi opphører for mange perioder`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode, besvart = LocalDateTime.now().minusYears(1))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, besvart = LocalDateTime.now().minusYears(1))
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(1.februar, 28.februar, 100.prosent))
        håndterYtelser(2.vedtaksperiode, besvart = LocalDateTime.now().minusYears(1))
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(1.mars, 31.mars, 100.prosent))
        håndterYtelser(3.vedtaksperiode, besvart = LocalDateTime.now().minusYears(1))
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt(3.vedtaksperiode)

        håndterOverstyring((20.januar til 26.januar).map { manuellFeriedag(it) })
        håndterYtelser(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.februar, 28.februar, 100.prosent, 1000.daglig),
            inntektshistorikk = listOf(
                Inntektsopplysning(
                    ORGNUMMER,
                    1.februar, INNTEKT, true
                )
            )
        )
        håndterYtelser(2.vedtaksperiode)
        håndterOverstyring((27.januar til 29.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)

        assertNotEquals(
            -44361,
            inspektør.utbetalinger.lastOrNull()?.arbeidsgiverOppdrag()?.nettoBeløp(),
            "Det ser ut som vi trekker tilbake hele januar og februar, her burde vi kun trekke tilbake feriedagene"
        )
    }

    @Test
    fun `Overstyring av utkast til revurdering sender senere perioder til AVVENTER_ARBEIDSGIVERE_REVURDERING`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)

        håndterOverstyring((20.januar til 29.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)

        // Denne overstyringen kommer før den forrige er ferdig prossessert
        håndterOverstyring((30.januar til 31.januar).map { manuellFeriedag(it) })

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING
        )

        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_ARBEIDSGIVERE_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_ARBEIDSGIVERE_REVURDERING
        )

        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_ARBEIDSGIVERE_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_ARBEIDSGIVERE_REVURDERING
        )
    }

    @Test
    fun `Overstyring av utkast til revurdering sender senere periode i AVVENTER_SIMULERING_REVURDERING til AVVENTER_ARBEIDSGIVERE_REVURDERING`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)

        håndterOverstyring((20.januar til 29.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterYtelser(3.vedtaksperiode)

        // Denne overstyringen kommer før den forrige er ferdig prossessert
        håndterOverstyring((30.januar til 31.januar).map { manuellFeriedag(it) })

        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING
        )

        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_ARBEIDSGIVERE_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_ARBEIDSGIVERE_REVURDERING
        )

        assertTilstander(
            3.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET,
            AVVENTER_ARBEIDSGIVERE_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_ARBEIDSGIVERE_REVURDERING
        )
    }

    @Test
    fun `Håndter påminnelser i alle tilstandene knyttet til en revurdering med en arbeidsgiver`() {
        nyttVedtak(1.januar, 31.januar)
        håndterOverstyring((25.januar til 26.januar).map { manuellFeriedag(it) })
        håndterPåminnelse(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        assertEtterspurteYtelser(4, 1.vedtaksperiode)

        håndterYtelser(1.vedtaksperiode)
        håndterPåminnelse(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)
        assertEquals(3, inspektør.antallEtterspurteBehov(1.vedtaksperiode, Aktivitetslogg.Aktivitet.Behov.Behovtype.Simulering))

        håndterSimulering(1.vedtaksperiode)
        håndterPåminnelse(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
        assertEquals(3, inspektør.antallEtterspurteBehov(1.vedtaksperiode, Aktivitetslogg.Aktivitet.Behov.Behovtype.Godkjenning))
    }

    @Test
    fun `validering av infotrygdhistorikk i revurdering skal føre til en warning i stedet for en feilet revurdering`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode, besvart = LocalDateTime.now().minusYears(1))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, besvart = LocalDateTime.now().minusYears(1))
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        håndterOverstyring((20.januar til 26.januar).map { manuellFeriedag(it) })

        håndterYtelser(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 31.januar, 100.prosent, 1000.daglig),
            inntektshistorikk = listOf(
                Inntektsopplysning(
                    ORGNUMMER,
                    17.januar, INNTEKT, true
                )
            )
        )

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)
        assertWarningTekst(
            inspektør,
            "Opplysninger fra Infotrygd har endret seg etter at vedtaket ble fattet. Undersøk om det er overlapp med periode fra Infotrygd.",
            "Utbetaling i Infotrygd overlapper med vedtaksperioden"
        )
    }

    @Test
    fun `Om en periode havner i RevurderingFeilet så skal alle sammenhengende perioder havne i RevurderingFeilet`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)

        håndterOverstyring((20.januar til 26.januar).map { manuellFeriedag(it) })

        håndterYtelser(1.vedtaksperiode, foreldrepenger = 17.januar til 31.januar)

        assertSisteTilstand(1.vedtaksperiode, REVURDERING_FEILET)
        assertSisteTilstand(2.vedtaksperiode, REVURDERING_FEILET)
        assertSisteTilstand(3.vedtaksperiode, REVURDERING_FEILET)
    }

    @Test
    fun `Om en revurdering blir stanses før den behandles så skal si fra at vi avviser den`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(1.mars, 31.mars)

        håndterOverstyring((31.januar til 2.mars).map { manuellFeriedag(it) })

        assertEquals(1, observatør.avvisteRevurderinger.size)
    }
}
