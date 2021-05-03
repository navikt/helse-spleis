package no.nav.helse.spleis.e2e

import no.nav.helse.Toggles
import no.nav.helse.hendelser.*
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.person.infotrygdhistorikk.Utbetalingsperiode
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class OverstyrerUtbetaltTidslinjeTest : AbstractEndToEndTest() {

    @BeforeEach
    fun tearup() {
        Toggles.RevurderUtbetaltPeriode.enable()
    }

    @AfterEach
    fun setdown() {
        Toggles.RevurderUtbetaltPeriode.pop()
    }

    @Test
    fun `to perioder - overstyr dager i eldste`() {
        nyttVedtak(3.januar, 26.januar)
        forlengVedtak(27.januar, 14.februar)

        håndterOverstyring((20.januar til 22.januar).map { manuellFeriedag(it) })
        håndterYtelser(2.vedtaksperiode)   // No history
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
        assertEquals(18, inspektør.forbrukteSykedager(2))
    }

    @Test
    fun `to perioder - overstyr dag i nyeste`() {
        nyttVedtak(3.januar, 26.januar)
        forlengVedtak(27.januar, 14.februar)

        håndterOverstyring((4.februar til 8.februar).map { manuellFeriedag(it) })
        håndterYtelser(2.vedtaksperiode)   // No history
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

        håndterOverstyring((3.januar til 26.februar).map { manuellFeriedag(it) })
        håndterYtelser(2.vedtaksperiode)   // No history
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
    fun `to perioder - hele den nyeste perioden blir ferie`() {
        nyttVedtak(3.januar, 26.januar)
        forlengVedtak(27.januar, 14.februar)

        håndterOverstyring((27.januar til 14.februar).map { manuellFeriedag(it) })
        håndterYtelser(2.vedtaksperiode)   // No history
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
    fun `to perioder - hele den eldste perioden blir sykedager`() { } //???? Skal hele den først perioden være en søknad med bare ferie..?

    @Test
    fun `kan ikke utvide perioden med sykedager`() {
        nyttVedtak(3.januar, 26.januar)
        håndterOverstyring((25.januar til 14.februar).map { manuellSykedag(it) })
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)

        assertTilstander(0,
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
    fun `forsøk på å flytte arbeidsgiverperioden`() { } // Hvordan gjør man det?

    @Test
    @Disabled
    fun `flere arbeidsgivere`() { assertTrue(false) } //idk

    @Test
    fun `overstyring fører ikke til endringer`() {

    }

    @Test
    fun `ferie i arbeidsgiverperiode`() {
        nyttVedtak(3.januar, 26.januar)
        håndterSykmelding(Sykmeldingsperiode(27.januar, 14.februar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(27.januar, 14.februar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)   // No history
        håndterSimulering(2.vedtaksperiode)

        håndterOverstyring((6.januar til 9.januar).map { manuellFeriedag(it) })  // ferie på første navdag
        håndterYtelser(1.vedtaksperiode)   // No history
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
            AVVENTER_HISTORIKK,
        )
        assertEquals(3, inspektør.utbetalinger.size)
        assertFalse( inspektør.utbetaling(2).harUtbetalinger())

    }

    @Test
    fun `ledende uferdig periode`() {
        nyttVedtak(3.januar, 26.januar)
        håndterSykmelding(Sykmeldingsperiode(27.januar, 14.februar, 100.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(27.januar, 14.februar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)   // No history
        håndterSimulering(2.vedtaksperiode)

        håndterOverstyring((19.januar til 22.januar).map { manuellFeriedag(it) })  // ferie på første navdag
        håndterYtelser(1.vedtaksperiode)   // No history
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
            AVVENTER_HISTORIKK,
        )
        val revurdering = inspektør.utbetaling(2)
        assertEquals(2, revurdering.arbeidsgiverOppdrag().size)
        assertEquals(19.januar, revurdering.arbeidsgiverOppdrag()[0].datoStatusFom())
        assertEquals(23.januar til 26.januar, revurdering.arbeidsgiverOppdrag()[1].periode)
    }

    @Test
    @Disabled
    fun `ledende uferdig periode som ikke har en utbetaling`() {
        nyttVedtak(3.januar, 26.januar)
        håndterSykmelding(Sykmeldingsperiode(27.januar, 14.februar, 100.prosent))

        håndterOverstyring((6.januar til 10.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)   // No history
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode) //Utbetalingene blir ikke tilbakestilt?

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

        assertTilstander(
            1,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE
        )

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
    fun `forsøk på å overstyre eldre fagsystemId med nyere periode til godkjenning`() {
        nyttVedtak(3.januar, 26.januar)
        tilSimulert(3.mars, 26.mars, 100.prosent, 3.mars)
        håndterOverstyring((4.januar til 20.januar).map { manuellFeriedag(it) })
        SykdomstidslinjeInspektør(inspektør.sykdomstidslinje).also { sykdomstidslinjeInspektør ->
            assertTrue((4.januar til 20.januar).none { sykdomstidslinjeInspektør.dager[it] == Dag.Feriedag::class })
        }
        assertTrue(hendelselogg.hasErrorsOrWorse())
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
            AVSLUTTET
        )
    }

    @Test
    fun `forsøk på å overstyre eldre fagsystemId med nyere perioder uten utbetaling, og periode med utbetaling etterpå`() {
        nyttVedtak(3.januar, 26.januar)
        tilSimulert(1.mai, 31.mai, 100.prosent, 1.mai)
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
    fun `overstyrer siste utbetalte periode med bare ferie og permisjon`() {
        nyttVedtak(3.januar, 26.januar)

        håndterOverstyring((3.januar til 20.januar).map { manuellFeriedag(it) } + (21.januar til 26.januar).map { manuellPermisjonsdag(it) })
        håndterYtelser(1.vedtaksperiode)   // No history
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

        assertEquals(2, inspektør.utbetalinger.size)
        assertEquals(6, inspektør.forbrukteSykedager(0))
        assertEquals(0, inspektør.forbrukteSykedager(1))
    }


    @Test
    fun `overstyring ville ha ledet til infotrygd - perioden faller tilbake til en trygg tilstand`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(2.januar, 18.januar)), førsteFraværsdag = 2.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100.prosent))
        håndterYtelser(1.vedtaksperiode, besvart = LocalDateTime.now().minusHours(24))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode, besvart = LocalDateTime.now().minusHours(24))
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(1.vedtaksperiode)

        håndterOverstyring((3.januar til 20.januar).map { manuellFeriedag(it) } + (21.januar til 26.januar).map { manuellPermisjonsdag(it) })
        håndterYtelser(
            1.vedtaksperiode,
            Utbetalingsperiode(ORGNUMMER, 3.januar til 26.januar, 100.prosent, 15000.daglig),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 3.januar, 15000.daglig, true))
        )

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
            AVSLUTTET_INGEN_ENDRING
        )
    }

    private fun manuellPermisjonsdag(dato: LocalDate) = ManuellOverskrivingDag(dato, Dagtype.Permisjonsdag)
    private fun manuellFeriedag(dato: LocalDate) = ManuellOverskrivingDag(dato, Dagtype.Feriedag)
    private fun manuellSykedag(dato: LocalDate) = ManuellOverskrivingDag(dato, Dagtype.Sykedag, 100)
}
