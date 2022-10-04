package no.nav.helse.spleis.e2e.revurdering

import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import no.nav.helse.Toggle.Companion.enable
import no.nav.helse.april
import no.nav.helse.assertForventetFeil
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype.Feriedag
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.Periodetype
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertUtbetalingsbeløp
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikk
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EnableToggle(Toggle.RevurderOutOfOrder::class)
internal class RevurderingOutOfOrderForlengelserTest : AbstractEndToEndTest() {

    @Test
    fun `skal kaste ut out-of-order søknad som flytter skjæringstidspunktet til annen periode`() = Toggle.RevurderOutOfOrderForlengelser.disable {
        nyttVedtak(1.februar, 28.februar)
        nyPeriode(1.januar til 31.januar)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(2.vedtaksperiode, TIL_INFOTRYGD)
    }

    @Test
    fun `skal kaste ut out-of-order søknad som flytter skjæringstidspunktet til annen periode - med revurdering in play`() = Toggle.RevurderOutOfOrderForlengelser.disable {
        nyttVedtak(1.februar, 28.februar)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(28.februar, Feriedag)))
        nyPeriode(1.januar til 31.januar)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        assertSisteTilstand(2.vedtaksperiode, TIL_INFOTRYGD)
    }

    @Test
    fun `skal kaste ut out-of-order søknad som flytter skjæringstidspunktet til annen periode - forlengelse`() = Toggle.RevurderOutOfOrderForlengelser.disable {
        nyttVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)
        nyPeriode(1.januar til 31.januar)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(3.vedtaksperiode, TIL_INFOTRYGD)
    }

    @Test
    fun `skal kaste ut out-of-order søknad som flytter skjæringstidspunktet til annen periode - flere arbeidsgivere`() = Toggle.RevurderOutOfOrderForlengelser.disable {
        nyttVedtak(1.februar, 28.februar, orgnummer = a1)
        nyPeriode(1.januar til 31.januar, a2)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD, orgnummer = a2)
    }

    @Test
    fun `to perioder på rad kommer out of order - skal revuderes i riktig rekkefølge`() = Toggle.RevurderOutOfOrderForlengelser.enable {
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
    fun `out-of-order søknad medfører revurdering -- Avsluttet`() = listOf(Toggle.RevurderOutOfOrderForlengelser, Toggle.ForkasteVilkårsgrunnlag).enable {
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
            forventetArbeidsgiverRefusjonsbeløp = 1615,
            subset = 1.februar til 28.februar
        )
    }

    @Test
    fun `en kort out-of-order søknad som flytter skjæringstidspunkt skal trigge revurdering`() = Toggle.RevurderOutOfOrderForlengelser.enable {
        nyttVedtak(1.februar, 28.februar)
        nyPeriode(20.januar til 31.januar)
        håndterUtbetalingshistorikk(2.vedtaksperiode)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertForventetFeil(
            forklaring = "skal revurderes fordi skjæringstidspunktet flyttes",
            nå = {
                assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            },
            ønsket = {
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
            }
        )
    }

    @Test
    fun `out-of-order søknad medfører revurdering -- AvsluttetUtenUtbetaling`() = Toggle.RevurderOutOfOrderForlengelser.enable {
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
    fun `out-of-order søknad medfører revurdering -- AvventerHistorikkRevurdering`() = Toggle.RevurderOutOfOrderForlengelser.enable {
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
    fun `out-of-order søknad medfører revurdering -- AvventerSimuleringRevurdering`() = Toggle.RevurderOutOfOrderForlengelser.enable {
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
    fun `out-of-order søknad medfører revurdering -- AvventerGodkjenningRevurdering`() = Toggle.RevurderOutOfOrderForlengelser.enable {
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
    fun `out-of-order søknad medfører revurdering -- AvventerGjennomførtRevurdering`() = Toggle.RevurderOutOfOrderForlengelser.enable {
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
    fun `out-of-order søknad medfører revurdering -- AvventerRevurdering`() = Toggle.RevurderOutOfOrderForlengelser.enable {
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
}