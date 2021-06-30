package no.nav.helse.spleis.e2e

import no.nav.helse.Toggles
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.TestInstance.Lifecycle
import java.time.LocalDate
import java.time.LocalDateTime

@TestInstance(Lifecycle.PER_CLASS)
internal class OverstyrerUtbetaltTidslinjeTest : AbstractEndToEndTest() {

    @BeforeAll
    fun beforeAll() {
        Toggles.RevurderTidligerePeriode.enable()
    }

    @AfterAll
    fun afterAll() {
        Toggles.RevurderTidligerePeriode.disable()
    }

    @Test
    fun `to perioder - overstyr dager i eldste`() {
        Toggles.RevurderTidligerePeriode.enable {
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
                AVVENTER_UTBETALINGSGRUNNLAG,
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
                AVVENTER_UTBETALINGSGRUNNLAG,
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
    }

    @Test
    fun `overstyring blør ikke ned i sykdomstidslinja på vedtaksperiodenivå`() {
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
        assertNull(inspektør.vedtaksperiodeDagTeller[1.vedtaksperiode]?.get(Dag.Feriedag::class))
        assertNull(inspektør.vedtaksperiodeDagTeller[2.vedtaksperiode]?.get(Dag.Feriedag::class))
    }

    @Test
    fun `to perioder - overstyr dag i nyeste`() {
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
            AVVENTER_UTBETALINGSGRUNNLAG,
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
            AVVENTER_UTBETALINGSGRUNNLAG,
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
            AVVENTER_UTBETALINGSGRUNNLAG,
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
            AVVENTER_UTBETALINGSGRUNNLAG,
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
            AVVENTER_UTBETALINGSGRUNNLAG,
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
        håndterUtbetalingsgrunnlag(2.vedtaksperiode)
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
            AVVENTER_UTBETALINGSGRUNNLAG,
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
            AVVENTER_UTBETALINGSGRUNNLAG,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            AVVENTER_UFERDIG_FORLENGELSE,
            AVVENTER_UTBETALINGSGRUNNLAG
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
        håndterUtbetalingsgrunnlag(2.vedtaksperiode)
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
            AVVENTER_UTBETALINGSGRUNNLAG,
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
            AVVENTER_UTBETALINGSGRUNNLAG,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            AVVENTER_UFERDIG_FORLENGELSE,
            AVVENTER_UTBETALINGSGRUNNLAG
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
            AVVENTER_UTBETALINGSGRUNNLAG,
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
    fun `forsøk på å overstyre eldre fagsystemId`() {
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
            AVVENTER_UTBETALINGSGRUNNLAG,
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
            AVVENTER_UTBETALINGSGRUNNLAG,
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
    fun `forsøk på å overstyre eldre fagsystemId med nyere periode til godkjenning`() {
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
            AVVENTER_UTBETALINGSGRUNNLAG,
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
            AVVENTER_UTBETALINGSGRUNNLAG,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING
        )
    }

    @Test
    fun `forsøk på å overstyre eldre fagsystemId med nyere perioder uten utbetaling, og periode med utbetaling etterpå`() {
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
            AVVENTER_UTBETALINGSGRUNNLAG,
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
            AVVENTER_UTBETALINGSGRUNNLAG,
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
    fun `forsøk på å overstyre eldre fagsystemId med nyere perioder med utbetaling, og periode uten utbetaling`() {
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
            AVVENTER_UTBETALINGSGRUNNLAG,
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
            AVVENTER_UTBETALINGSGRUNNLAG,
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
    fun `overstyrer siste utbetalte periode med bare ferie og permisjon`() {
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
            AVVENTER_UTBETALINGSGRUNNLAG,
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
    fun `Feil i validering av infotrygdhistorikk fører til feilet revurdering`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 17.januar)), førsteFraværsdag = 1.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 31.januar, 100.prosent))
        håndterUtbetalingsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, besvart = LocalDateTime.now().minusHours(24))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode, besvart = LocalDateTime.now().minusHours(24))
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode)

        håndterOverstyring(listOf(manuellFeriedag(18.januar)))
        håndterYtelser(
            1.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 20.januar, 100.prosent, 15000.daglig),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, 15000.daglig, true)),
            besvart = LocalDateTime.now()
        )
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_UTBETALINGSGRUNNLAG,
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
        assertWarn("Validering av ytelser ved revurdering feilet. Utbetalingen må annulleres", inspektør.personLogg)
    }

    @Test
    fun `Avslag fører til feilet revurdering`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 17.januar)), førsteFraværsdag = 1.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 31.januar, 100.prosent))
        håndterUtbetalingsgrunnlag(1.vedtaksperiode)
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
            AVVENTER_UTBETALINGSGRUNNLAG,
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
        håndterUtbetalingsgrunnlag(1.vedtaksperiode)
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
            AVVENTER_UTBETALINGSGRUNNLAG,
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
        håndterUtbetalingsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, besvart = LocalDateTime.now().minusHours(24))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode, besvart = LocalDateTime.now().minusHours(24))
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode)

        håndterOverstyring(listOf(manuellFeriedag(18.januar)))
        håndterYtelser(
            1.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 20.januar, 100.prosent, 15000.daglig),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, 15000.daglig, true)),
            besvart = LocalDateTime.now()
        )

        håndterAnnullerUtbetaling()

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_UTBETALINGSGRUNNLAG,
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
            AVVENTER_UTBETALINGSGRUNNLAG,
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
            AVVENTER_UTBETALINGSGRUNNLAG
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
            AVVENTER_UTBETALINGSGRUNNLAG,
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
    @Suppress("UNCHECKED_CAST")
    fun `overstyre inn ferie med forgående periode med avsluttende ferie`() {
        /* Hvis forgående periode avsluttes med ferie og vi revurderer starten av etterfølgende periode med ferie blir det feil i utgående behov.
           Da får vi ikke med oss at feriedagene skal trekkes tilbake */
        håndterSykmelding(Sykmeldingsperiode(1.april(2021), 23.april(2021), 100.prosent))
        håndterSøknadMedValidering(
            1.vedtaksperiode,
            Sykdom(1.april(2021), 23.april(2021), 100.prosent),
            Ferie(23.april(2021), 23.april(2021))
        )
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.april(2021), 16.april(2021))), førsteFraværsdag = 1.april(2021))
        håndterUtbetalingsgrunnlag(1.vedtaksperiode)
        håndterYtelser(
            1.vedtaksperiode,
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2020) til 1.mars(2021) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(
            1.vedtaksperiode,
        )
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode)
        forlengVedtak(24.april(2021), 7.mai(2021))

        håndterOverstyring((26.april(2021) til 27.april(2021)).map { manuellFeriedag(it) })
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        val behov = inspektør.sisteBehov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling)
        assertEquals(2, (behov.detaljer()["linjer"] as List<*>).size)

        assertEquals("2021-04-24", (behov.detaljer()["linjer"] as List<Map<String, Any>>)[0]["fom"])
        assertEquals("2021-05-07", (behov.detaljer()["linjer"] as List<Map<String, Any>>)[0]["tom"])
        assertEquals("2021-04-24", (behov.detaljer()["linjer"] as List<Map<String, Any>>)[0]["datoStatusFom"])
        assertEquals("OPPH", (behov.detaljer()["linjer"] as List<Map<String, Any>>)[0]["statuskode"])
        assertEquals("2021-04-28", (behov.detaljer()["linjer"] as List<Map<String, Any>>)[1]["fom"])
        assertEquals("2021-05-07", (behov.detaljer()["linjer"] as List<Map<String, Any>>)[1]["tom"])
        assertWarnings(inspektør)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `overstyre inn ferie med forgående periode uten avsluttende ferie`() {
        /* Hvis forgående periode avsluttes med ferie og vi revurderer starten av etterfølgende periode med ferie blir det feil i utgående behov.
           Da får vi ikke med oss at feriedagene skal trekkes tilbake */
        håndterSykmelding(Sykmeldingsperiode(1.april(2021), 23.april(2021), 100.prosent))
        håndterSøknadMedValidering(
            1.vedtaksperiode,
            Sykdom(1.april(2021), 23.april(2021), 100.prosent)
        )
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.april(2021), 16.april(2021))), førsteFraværsdag = 1.april(2021))
        håndterUtbetalingsgrunnlag(1.vedtaksperiode)
        håndterYtelser(
            1.vedtaksperiode,
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.januar(2020) til 1.mars(2021) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(
            1.vedtaksperiode,
        )
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode)
        forlengVedtak(24.april(2021), 7.mai(2021))

        håndterOverstyring((26.april(2021) til 27.april(2021)).map { manuellFeriedag(it) })
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        val behov = inspektør.sisteBehov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling)
        assertEquals(2, (behov.detaljer()["linjer"] as List<*>).size)

        assertEquals("2021-04-17", (behov.detaljer()["linjer"] as List<Map<String, Any>>)[0]["fom"])
        assertEquals("2021-04-25", (behov.detaljer()["linjer"] as List<Map<String, Any>>)[0]["tom"])
        assertEquals("2021-04-28", (behov.detaljer()["linjer"] as List<Map<String, Any>>)[1]["fom"])
        assertEquals("2021-05-07", (behov.detaljer()["linjer"] as List<Map<String, Any>>)[1]["tom"])
    }

    private fun manuellPermisjonsdag(dato: LocalDate) = ManuellOverskrivingDag(dato, Dagtype.Permisjonsdag)
    private fun manuellFeriedag(dato: LocalDate) = ManuellOverskrivingDag(dato, Dagtype.Feriedag)
    private fun manuellSykedag(dato: LocalDate) = ManuellOverskrivingDag(dato, Dagtype.Sykedag, 100)
}
