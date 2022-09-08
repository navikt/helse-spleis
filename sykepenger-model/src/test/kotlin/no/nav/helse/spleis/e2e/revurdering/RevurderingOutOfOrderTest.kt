package no.nav.helse.spleis.e2e.revurdering

import java.time.LocalDate
import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import no.nav.helse.april
import no.nav.helse.assertForventetFeil
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype.Feriedag
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.Periodetype
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertForkastetPeriodeTilstander
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertUtbetalingsbeløp
import no.nav.helse.spleis.e2e.finnSkjæringstidspunkt
import no.nav.helse.spleis.e2e.forlengTilGodkjenning
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.forlengelseTilGodkjenning
import no.nav.helse.spleis.e2e.grunnlag
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikk
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.spleis.e2e.repeat
import no.nav.helse.spleis.e2e.sammenligningsgrunnlag
import no.nav.helse.spleis.e2e.tilGodkjent
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EnableToggle(Toggle.RevurderOutOfOrder::class)
internal class RevurderingOutOfOrderTest : AbstractEndToEndTest() {

    @Test
    fun `out-of-order søknad medfører revurdering -- Avsluttet`() {
        nyttVedtak(1.februar, 28.februar)
        nyPeriode(1.januar til 31.januar)

        val februarId = 1.vedtaksperiode
        val januarId = 2.vedtaksperiode

        assertSisteTilstand(februarId, AVVENTER_REVURDERING)
        assertSisteTilstand(januarId, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)

        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 35000.månedlig)
        håndterYtelser(januarId)
        håndterVilkårsgrunnlag(januarId)
        håndterYtelser(januarId)
        håndterSimulering(januarId)
        håndterUtbetalingsgodkjenning(januarId)
        håndterUtbetalt()

        assertSisteTilstand(februarId, AVVENTER_HISTORIKK_REVURDERING)
        assertSisteTilstand(januarId, AVSLUTTET)
        håndterYtelser(februarId)
        assertForventetFeil(
            forklaring = "Februar-perioden beregner ikke utbetaling utifra det nye vilkårsgrunnlaget på skjæringstidspunkt 1.januar",
            nå = {
                håndterSimulering(februarId)
                håndterUtbetalingsgodkjenning(februarId)
                håndterUtbetalt()
                assertEquals(1.februar, inspektør.skjæringstidspunkt(februarId))
                assertUtbetalingsbeløp(
                    januarId,
                    forventetArbeidsgiverbeløp = 1615,
                    forventetArbeidsgiverRefusjonsbeløp = 1615,
                    subset = 17.januar til 31.januar
                )
                assertUtbetalingsbeløp(
                    februarId,
                    forventetArbeidsgiverbeløp = 1431,
                    forventetArbeidsgiverRefusjonsbeløp = 1615,
                    subset = 1.februar til 28.februar
                )
            },
            ønsket = {
                håndterVilkårsgrunnlag(februarId)
                håndterYtelser(februarId)
                håndterSimulering(februarId)
                håndterUtbetalingsgodkjenning(februarId)
                håndterUtbetalt()
                assertEquals(1.januar, inspektør.skjæringstidspunkt(februarId))
                assertUtbetalingsbeløp(
                    januarId,
                    forventetArbeidsgiverbeløp = 1615,
                    forventetArbeidsgiverRefusjonsbeløp = 1615,
                    subset = 17.januar til 31.januar
                )
                assertUtbetalingsbeløp(
                    februarId,
                    forventetArbeidsgiverbeløp = 1615,
                    forventetArbeidsgiverRefusjonsbeløp = 1431,
                    subset = 1.februar til 28.februar
                )
            }
        )
    }

    @Test
    fun `out-of-order søknad medfører revurdering -- AvsluttetUtenUtbetaling`() {
        nyPeriode(1.februar til 10.februar)
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        nyPeriode(1.januar til 31.januar)

        val februarId = 1.vedtaksperiode
        val januarId = 2.vedtaksperiode

        assertSisteTilstand(februarId, AVVENTER_REVURDERING)
        assertSisteTilstand(januarId, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)

        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(januarId)
        håndterVilkårsgrunnlag(januarId)
        håndterYtelser(januarId)
        håndterSimulering(januarId)
        håndterUtbetalingsgodkjenning(januarId)
        håndterUtbetalt()

        assertSisteTilstand(februarId, AVVENTER_HISTORIKK_REVURDERING)
        assertSisteTilstand(januarId, AVSLUTTET)

        håndterYtelser(februarId)
        håndterSimulering(februarId)
        håndterUtbetalingsgodkjenning(februarId)
        håndterUtbetalt()

        assertSisteTilstand(januarId, AVSLUTTET)
        assertSisteTilstand(februarId, AVSLUTTET)
        assertUtbetalingsbeløp(
            januarId,
            forventetArbeidsgiverbeløp = 1431,
            forventetArbeidsgiverRefusjonsbeløp = 1431,
            subset = 17.januar til 31.januar
        )
        assertUtbetalingsbeløp(
            februarId,
            forventetArbeidsgiverbeløp = 1431,
            forventetArbeidsgiverRefusjonsbeløp = 1431,
            subset = 1.februar til 28.februar
        )
    }

    @Test
    fun `out-of-order søknad medfører revurdering -- AvventerHistorikkRevurdering`() {
        nyttVedtak(1.februar, 28.februar)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(28.februar, Feriedag)))
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)

        nyPeriode(1.januar til 31.januar)

        val februarId = 1.vedtaksperiode
        val januarId = 2.vedtaksperiode

        assertSisteTilstand(februarId, AVVENTER_REVURDERING)
        assertSisteTilstand(januarId, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)

        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(januarId)
        håndterVilkårsgrunnlag(januarId)
        håndterYtelser(januarId)
        håndterSimulering(januarId)
        håndterUtbetalingsgodkjenning(januarId)
        håndterUtbetalt()

        assertSisteTilstand(februarId, AVVENTER_HISTORIKK_REVURDERING)
        assertSisteTilstand(januarId, AVSLUTTET)

        håndterYtelser(februarId)
        håndterSimulering(februarId)
        håndterUtbetalingsgodkjenning(februarId)
        håndterUtbetalt()

        assertSisteTilstand(januarId, AVSLUTTET)
        assertSisteTilstand(februarId, AVSLUTTET)
        assertUtbetalingsbeløp(
            januarId,
            forventetArbeidsgiverbeløp = 1431,
            forventetArbeidsgiverRefusjonsbeløp = 1431,
            subset = 17.januar til 31.januar
        )
        assertUtbetalingsbeløp(
            februarId,
            forventetArbeidsgiverbeløp = 1431,
            forventetArbeidsgiverRefusjonsbeløp = 1431,
            subset = 1.februar til 27.februar
        )
        assertUtbetalingsbeløp(
            februarId,
            forventetArbeidsgiverbeløp = 0, // denne dagen ble overstyrt til ferie
            forventetArbeidsgiverRefusjonsbeløp = 1431,
            subset = 28.februar til 28.februar
        )
    }

    @Test
    fun `out-of-order søknad medfører revurdering -- AvventerVilksprøvingRevurdering`() {
        nyPeriode(1.februar til 10.februar)
        håndterUtbetalingshistorikk(1.vedtaksperiode, besvart = LocalDate.EPOCH.atStartOfDay())
        håndterInntektsmelding(arbeidsgiverperioder = listOf(20.januar til 4.februar), førsteFraværsdag = 20.januar)
        håndterYtelser(1.vedtaksperiode, besvart = LocalDate.EPOCH.atStartOfDay())
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING_REVURDERING)

        nyPeriode(1.januar til 15.januar)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)

        håndterUtbetalingshistorikk(2.vedtaksperiode)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        assertUtbetalingsbeløp(
            1.vedtaksperiode,
            forventetArbeidsgiverbeløp = 1431,
            forventetArbeidsgiverRefusjonsbeløp = 1431,
            subset = 5.februar til 10.februar
        )
    }

    @Test
    fun `out-of-order søknad medfører revurdering -- AvventerSimuleringRevurdering`() {
        nyttVedtak(1.februar, 28.februar)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(28.februar, Feriedag)))
        håndterYtelser(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)

        nyPeriode(1.januar til 31.januar)

        val februarId = 1.vedtaksperiode
        val januarId = 2.vedtaksperiode

        assertSisteTilstand(februarId, AVVENTER_REVURDERING)
        assertSisteTilstand(januarId, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)

        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(januarId)
        håndterVilkårsgrunnlag(januarId)
        håndterYtelser(januarId)
        håndterSimulering(januarId)
        håndterUtbetalingsgodkjenning(januarId)
        håndterUtbetalt()

        assertSisteTilstand(februarId, AVVENTER_HISTORIKK_REVURDERING)
        assertSisteTilstand(januarId, AVSLUTTET)

        håndterYtelser(februarId)
        håndterSimulering(februarId)
        håndterUtbetalingsgodkjenning(februarId)
        håndterUtbetalt()

        assertSisteTilstand(januarId, AVSLUTTET)
        assertSisteTilstand(februarId, AVSLUTTET)
        assertUtbetalingsbeløp(
            januarId,
            forventetArbeidsgiverbeløp = 1431,
            forventetArbeidsgiverRefusjonsbeløp = 1431,
            subset = 17.januar til 31.januar
        )
        assertUtbetalingsbeløp(
            februarId,
            forventetArbeidsgiverbeløp = 1431,
            forventetArbeidsgiverRefusjonsbeløp = 1431,
            subset = 1.februar til 27.februar
        )
        assertUtbetalingsbeløp(
            februarId,
            forventetArbeidsgiverbeløp = 0, // denne dagen ble overstyrt til ferie
            forventetArbeidsgiverRefusjonsbeløp = 1431,
            subset = 28.februar til 28.februar
        )
    }
    @Test
    fun `out-of-order søknad medfører revurdering -- AvventerGodkjenningRevurdering`() {
        nyttVedtak(1.februar, 28.februar)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(28.februar, Feriedag)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)

        nyPeriode(1.januar til 31.januar)

        val februarId = 1.vedtaksperiode
        val januarId = 2.vedtaksperiode

        assertSisteTilstand(februarId, AVVENTER_REVURDERING)
        assertSisteTilstand(januarId, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)

        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(januarId)
        håndterVilkårsgrunnlag(januarId)
        håndterYtelser(januarId)
        håndterSimulering(januarId)
        håndterUtbetalingsgodkjenning(januarId)
        håndterUtbetalt()

        assertSisteTilstand(februarId, AVVENTER_HISTORIKK_REVURDERING)
        assertSisteTilstand(januarId, AVSLUTTET)

        håndterYtelser(februarId)
        håndterSimulering(februarId)
        håndterUtbetalingsgodkjenning(februarId)
        håndterUtbetalt()

        assertSisteTilstand(januarId, AVSLUTTET)
        assertSisteTilstand(februarId, AVSLUTTET)
        assertUtbetalingsbeløp(
            januarId,
            forventetArbeidsgiverbeløp = 1431,
            forventetArbeidsgiverRefusjonsbeløp = 1431,
            subset = 17.januar til 31.januar
        )
        assertUtbetalingsbeløp(
            februarId,
            forventetArbeidsgiverbeløp = 1431,
            forventetArbeidsgiverRefusjonsbeløp = 1431,
            subset = 1.februar til 27.februar
        )
        assertUtbetalingsbeløp(
            februarId,
            forventetArbeidsgiverbeløp = 0, // denne dagen ble overstyrt til ferie
            forventetArbeidsgiverRefusjonsbeløp = 1431,
            subset = 28.februar til 28.februar
        )
    }

    @Test
    fun `out-of-order søknad medfører revurdering -- AvventerGjennomførtRevurdering`() {
        nyttVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(28.februar, Feriedag)))
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)

        nyPeriode(1.januar til 31.januar)

        val februarId = 1.vedtaksperiode
        val marsId = 2.vedtaksperiode
        val januarId = 3.vedtaksperiode

        assertSisteTilstand(februarId, AVVENTER_REVURDERING)
        assertSisteTilstand(marsId, AVVENTER_REVURDERING)
        assertSisteTilstand(januarId, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)

        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(januarId)
        håndterVilkårsgrunnlag(januarId)
        håndterYtelser(januarId)
        håndterSimulering(januarId)
        håndterUtbetalingsgodkjenning(januarId)
        håndterUtbetalt()

        assertSisteTilstand(februarId, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertSisteTilstand(marsId, AVVENTER_HISTORIKK_REVURDERING)
        assertSisteTilstand(januarId, AVSLUTTET)

        håndterYtelser(marsId)
        håndterSimulering(marsId)
        håndterUtbetalingsgodkjenning(marsId)
        håndterUtbetalt()

        assertSisteTilstand(januarId, AVSLUTTET)
        assertSisteTilstand(februarId, AVSLUTTET)
        assertSisteTilstand(marsId, AVSLUTTET)
        assertUtbetalingsbeløp(
            januarId,
            forventetArbeidsgiverbeløp = 1431,
            forventetArbeidsgiverRefusjonsbeløp = 1431,
            subset = 17.januar til 31.januar
        )
        assertUtbetalingsbeløp(
            februarId,
            forventetArbeidsgiverbeløp = 1431,
            forventetArbeidsgiverRefusjonsbeløp = 1431,
            subset = 1.februar til 27.februar
        )
        assertUtbetalingsbeløp(
            februarId,
            forventetArbeidsgiverbeløp = 0, // denne dagen ble overstyrt til ferie
            forventetArbeidsgiverRefusjonsbeløp = 1431,
            subset = 28.februar til 28.februar
        )
        assertUtbetalingsbeløp(
            marsId,
            forventetArbeidsgiverbeløp = 1431,
            forventetArbeidsgiverRefusjonsbeløp = 1431,
            subset = 1.mars til 31.mars
        )
    }

    @Test
    fun `out-of-order søknad medfører revurdering -- AvventerRevurdering`() {
        nyttVedtak(1.februar, 28.februar)
        nyttVedtak(1.april, 30.april)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(28.februar, Feriedag)))
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)

        nyPeriode(1.mars til 31.mars)

        val februarId = 1.vedtaksperiode
        val aprilId = 2.vedtaksperiode
        val marsId = 3.vedtaksperiode

        assertSisteTilstand(februarId, AVVENTER_HISTORIKK_REVURDERING)
        assertSisteTilstand(marsId, AVVENTER_BLOKKERENDE_PERIODE)
        assertSisteTilstand(aprilId, AVVENTER_REVURDERING)

        håndterYtelser(februarId)
        håndterSimulering(februarId)
        håndterUtbetalingsgodkjenning(februarId)
        håndterUtbetalt()

        assertSisteTilstand(februarId, AVSLUTTET)
        assertSisteTilstand(marsId, AVVENTER_HISTORIKK)
        assertSisteTilstand(aprilId, AVVENTER_REVURDERING)

        håndterYtelser(marsId)
        håndterSimulering(marsId)
        håndterUtbetalingsgodkjenning(marsId)
        håndterUtbetalt()

        assertSisteTilstand(februarId, AVSLUTTET)
        assertSisteTilstand(marsId, AVSLUTTET)
        assertSisteTilstand(aprilId, AVVENTER_HISTORIKK_REVURDERING)

        håndterYtelser(aprilId)
        håndterSimulering(aprilId)
        håndterUtbetalingsgodkjenning(aprilId)
        håndterUtbetalt()

        assertSisteTilstand(februarId, AVSLUTTET)
        assertSisteTilstand(marsId, AVSLUTTET)
        assertTilstander(
            aprilId,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVVENTER_GODKJENNING_REVURDERING,
            TIL_UTBETALING,
            AVSLUTTET
        )

        assertUtbetalingsbeløp(
            februarId,
            forventetArbeidsgiverbeløp = 0,
            forventetArbeidsgiverRefusjonsbeløp = 1431,
            subset = 1.februar til 16.februar
        )
        assertUtbetalingsbeløp(
            februarId,
            forventetArbeidsgiverbeløp = 1431,
            forventetArbeidsgiverRefusjonsbeløp = 1431,
            subset = 17.februar til 27.februar
        )
        assertUtbetalingsbeløp(
            februarId,
            forventetArbeidsgiverbeløp = 0, // denne dagen ble overstyrt til ferie
            forventetArbeidsgiverRefusjonsbeløp = 1431,
            subset = 28.februar til 28.februar
        )
        assertUtbetalingsbeløp(
            marsId,
            forventetArbeidsgiverbeløp = 1431,
            forventetArbeidsgiverRefusjonsbeløp = 1431,
            subset = 1.mars til 31.mars
        )
        assertUtbetalingsbeløp(
            aprilId,
            forventetArbeidsgiverbeløp = 1431,
            forventetArbeidsgiverRefusjonsbeløp = 1431,
            subset = 1.april til 30.april
        )
    }

    @Test
    fun `out of order periode trigger revurdering`() {
        nyttVedtak(1.mai, 31.mai)
        forlengVedtak(1.juni, 30.juni)
        nullstillTilstandsendringer()
        nyttVedtak(1.januar, 31.januar)
        assertSisteTilstand(3.vedtaksperiode, AVSLUTTET)
        assertTilstander(
            1.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING
        )
        assertTilstander(
            2.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING
        )
    }

    @Test
    fun `out of order periode uten utbetaling trigger revurdering`() {
        nyttVedtak(1.mai, 31.mai)
        forlengVedtak(1.juni, 30.juni)
        nullstillTilstandsendringer()
        nyPeriode(1.januar til 15.januar)
        håndterUtbetalingshistorikk(3.vedtaksperiode)
        assertTilstander(
            3.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVSLUTTET_UTEN_UTBETALING
        )
        assertTilstander(
            1.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING
        )
        assertTilstander(
            2.vedtaksperiode,
            AVSLUTTET,
            AVVENTER_REVURDERING,
            AVVENTER_GJENNOMFØRT_REVURDERING,
            AVVENTER_HISTORIKK_REVURDERING
        )
    }

    @Test
    fun `revurdering av senere frittstående periode hos ag3 mens overlappende out of order hos ag1 og ag2 utbetales`() {
        håndterSykmelding(Sykmeldingsperiode(1.april, 30.april, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.april, 30.april, 100.prosent), orgnummer = a1)
        val inntekt = 20000.månedlig
        håndterInntektsmelding(listOf(1.april til 16.april), beregnetInntekt = inntekt, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        val sammenligningsgrunnlag = Inntektsvurdering(
            listOf(
                sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(12)),
                sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(12)),
                sammenligningsgrunnlag(a3, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(12))
            )
        )
        val sykepengegrunnlag = InntektForSykepengegrunnlag(
            listOf(
                grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(3)),
                grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(3)),
                grunnlag(a3, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(3))
            ), emptyList()
        )
        val arbeidsforhold = listOf(
            Arbeidsforhold(a1, LocalDate.EPOCH, null),
            Arbeidsforhold(a2, LocalDate.EPOCH, null),
            Arbeidsforhold(a3, LocalDate.EPOCH, null)
        )
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = sammenligningsgrunnlag,
            inntektsvurderingForSykepengegrunnlag = sykepengegrunnlag,
            arbeidsforhold = arbeidsforhold,
            orgnummer = a1
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt()


        nyPeriode(1.februar til 28.februar, a2, a3)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a3)

        håndterInntektsmelding(listOf(1.februar til 16.februar), beregnetInntekt = inntekt, orgnummer = a2)
        håndterInntektsmelding(listOf(1.februar til 16.februar), beregnetInntekt = inntekt, orgnummer = a3)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)

        val sammenligningsgrunnlag2 = Inntektsvurdering(
            listOf(
                sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), inntekt.repeat(12)),
                sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), inntekt.repeat(12)),
                sammenligningsgrunnlag(a3, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), inntekt.repeat(12))
            )
        )
        val sykepengegrunnlag2 = InntektForSykepengegrunnlag(
            listOf(
                grunnlag(a1, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), inntekt.repeat(3)),
                grunnlag(a2, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), inntekt.repeat(3)),
                grunnlag(a3, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), inntekt.repeat(3))
            ), emptyList()
        )
        val arbeidsforhold2 = listOf(
            Arbeidsforhold(a1, LocalDate.EPOCH, null),
            Arbeidsforhold(a2, LocalDate.EPOCH, null),
            Arbeidsforhold(a3, LocalDate.EPOCH, null)
        )
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = sammenligningsgrunnlag2,
            inntektsvurderingForSykepengegrunnlag = sykepengegrunnlag2,
            arbeidsforhold = arbeidsforhold2,
            orgnummer = a2
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a3)

        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a3)
    }

    @Test
    fun `revurdering av senere frittstående periode hos ag3 mens overlappende out of order hos ag1 og ag2 utbetales -- forlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(1.april, 30.april, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.april, 30.april, 100.prosent), orgnummer = a1)
        val inntekt = 20000.månedlig
        håndterInntektsmelding(listOf(1.april til 16.april), beregnetInntekt = inntekt, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        val sammenligningsgrunnlag = Inntektsvurdering(
            listOf(
                sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(12)),
                sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(12)),
                sammenligningsgrunnlag(a3, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(12))
            )
        )
        val sykepengegrunnlag = InntektForSykepengegrunnlag(
            listOf(
                grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(3)),
                grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(3)),
                grunnlag(a3, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), inntekt.repeat(3))
            ), emptyList()
        )
        val arbeidsforhold = listOf(
            Arbeidsforhold(a1, LocalDate.EPOCH, null),
            Arbeidsforhold(a2, LocalDate.EPOCH, null),
            Arbeidsforhold(a3, LocalDate.EPOCH, null)
        )
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = sammenligningsgrunnlag,
            inntektsvurderingForSykepengegrunnlag = sykepengegrunnlag,
            arbeidsforhold = arbeidsforhold,
            orgnummer = a1
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt()

        nyPeriode(1.februar til 28.februar, a2, a3)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a3)

        håndterInntektsmelding(listOf(1.februar til 16.februar), beregnetInntekt = inntekt, orgnummer = a2)
        håndterInntektsmelding(listOf(1.februar til 16.februar), beregnetInntekt = inntekt, orgnummer = a3)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)

        val sammenligningsgrunnlag2 = Inntektsvurdering(
            listOf(
                sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), inntekt.repeat(12)),
                sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), inntekt.repeat(12)),
                sammenligningsgrunnlag(a3, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), inntekt.repeat(12))
            )
        )
        val sykepengegrunnlag2 = InntektForSykepengegrunnlag(
            listOf(
                grunnlag(a1, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), inntekt.repeat(3)),
                grunnlag(a2, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), inntekt.repeat(3)),
                grunnlag(a3, finnSkjæringstidspunkt(a2, 1.vedtaksperiode), inntekt.repeat(3))
            ), emptyList()
        )
        val arbeidsforhold2 = listOf(
            Arbeidsforhold(a1, LocalDate.EPOCH, null),
            Arbeidsforhold(a2, LocalDate.EPOCH, null),
            Arbeidsforhold(a3, LocalDate.EPOCH, null)
        )
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = sammenligningsgrunnlag2,
            inntektsvurderingForSykepengegrunnlag = sykepengegrunnlag2,
            arbeidsforhold = arbeidsforhold2,
            orgnummer = a2
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        håndterYtelser(1.vedtaksperiode, orgnummer = a3)
        håndterSimulering(1.vedtaksperiode, orgnummer = a3)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a3)
        håndterUtbetalt(orgnummer = a3)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a3)

        forlengelseTilGodkjenning(1.mars, 15.mars, a2, a3)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a2)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a3)
        assertSisteTilstand(2.vedtaksperiode, TIL_UTBETALING, orgnummer = a2)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a3)
    }

    @Test
    fun `out of order periode mens senere periode revurderes til utbetaling`() {
        nyttVedtak(1.mai, 31.mai)
        forlengTilGodkjenning(1.juni, 30.juni)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        nyPeriode(1.januar til 31.januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        nullstillTilstandsendringer()

        assertForventetFeil(
            forklaring = """Forventer at periode som går fra til utbetaling til avsluttet blir sendt videre til 
                    avventer revurdering dersom tidligere periode gjenopptar behandling""",
            nå = {
                assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING)
                assertTilstander(2.vedtaksperiode, TIL_UTBETALING)
                assertSisteTilstand(3.vedtaksperiode, TIL_INFOTRYGD)

                håndterUtbetalt()

                assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING)
                assertTilstander(2.vedtaksperiode, TIL_UTBETALING, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
                assertSisteTilstand(3.vedtaksperiode, TIL_INFOTRYGD)
            },
            ønsket = {
                assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING)
                assertTilstander(2.vedtaksperiode, TIL_UTBETALING)
                assertSisteTilstand(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)

                håndterUtbetalt()

                assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING)
                assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING)
                assertSisteTilstand(3.vedtaksperiode, AVVENTER_HISTORIKK)
            }
        )
    }

    @Test
    fun `to perioder på rad kommer out of order - skal revuderes i riktig rekkefølge`() {
        val marsId = 1.vedtaksperiode
        nyttVedtak(1.mars, 31.mars)
        assertTilstand(marsId, AVSLUTTET)

        nyPeriode(1.februar til 28.februar)
        håndterInntektsmelding(listOf(1.februar til 16.februar))
        val februarId = 2.vedtaksperiode
        håndterYtelser(februarId)
        håndterVilkårsgrunnlag(februarId)
        håndterYtelser(februarId)
        håndterSimulering(februarId)
        håndterUtbetalingsgodkjenning(februarId)
        håndterUtbetalt()

        håndterYtelser(marsId)
        håndterSimulering(marsId)
        assertTilstand(marsId, AVVENTER_GODKJENNING_REVURDERING)
        assertTilstand(februarId, AVSLUTTET)


        nyPeriode(1.januar til 31.januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        val januarId = 3.vedtaksperiode
        håndterYtelser(januarId)
        håndterVilkårsgrunnlag(januarId)
        håndterYtelser(januarId)
        håndterSimulering(januarId)
        håndterUtbetalingsgodkjenning(januarId)
        håndterUtbetalt()

        assertEquals(Periodetype.FØRSTEGANGSBEHANDLING, inspektør.vedtaksperioder(januarId).inspektør.periodetype)
        assertEquals(Periodetype.FORLENGELSE, inspektør.vedtaksperioder(februarId).inspektør.periodetype)
        assertEquals(Periodetype.FORLENGELSE, inspektør.vedtaksperioder(marsId).inspektør.periodetype)

        assertForventetFeil(

            forklaring = "",
            nå = {
                assertTilstand(januarId, AVSLUTTET)
                assertTilstand(februarId, AVVENTER_HISTORIKK_REVURDERING) // Når denne utbetales feiler det fordi betalingen allerede er sendt til oppdrag (?)
                assertTilstand(marsId, AVVENTER_REVURDERING)
            },
            ønsket = {
                assertTilstand(januarId, AVSLUTTET)
                assertTilstand(februarId, AVVENTER_GJENNOMFØRT_REVURDERING)
                assertTilstand(marsId, AVVENTER_HISTORIKK_REVURDERING)
            },
        )
    }

    @Test
    fun `første periode i til utbetaling når det dukker opp en out of order-periode`() {
        tilGodkjent(1.mars, 31.mars, 100.prosent, 1.mars)
        nyPeriode(1.januar til 31.januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        nullstillTilstandsendringer()

        assertTilstander(1.vedtaksperiode, TIL_UTBETALING)
        håndterUtbetalt()

        assertForventetFeil(
            forklaring = """Forventer at periode som går fra til utbetaling til avsluttet blir sendt videre til 
                    avventer revurdering dersom tidligere periode gjenopptar behandling""",
            nå = {
                assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET)
                assertForkastetPeriodeTilstander(2.vedtaksperiode, TIL_INFOTRYGD)
            },
            ønsket = {
                assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET, AVVENTER_REVURDERING)
                assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)

                håndterYtelser(2.vedtaksperiode)
                håndterVilkårsgrunnlag(2.vedtaksperiode)
                håndterYtelser(2.vedtaksperiode)
                håndterSimulering(2.vedtaksperiode)
                håndterUtbetalingsgodkjenning(2.vedtaksperiode)
                håndterUtbetalt()

                assertTilstander(
                    2.vedtaksperiode,
                    AVVENTER_BLOKKERENDE_PERIODE,
                    AVVENTER_HISTORIKK,
                    AVVENTER_VILKÅRSPRØVING,
                    AVVENTER_HISTORIKK,
                    AVVENTER_SIMULERING,
                    AVVENTER_GODKJENNING,
                    TIL_UTBETALING,
                    AVSLUTTET
                )
            }
        )
    }
}