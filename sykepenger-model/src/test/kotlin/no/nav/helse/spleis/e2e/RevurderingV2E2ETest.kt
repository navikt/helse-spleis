package no.nav.helse.spleis.e2e

import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import no.nav.helse.april
import no.nav.helse.assertForventetFeil
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype.Feriedag
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER
import no.nav.helse.person.TilstandType.AVVENTER_UFERDIG
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@EnableToggle(Toggle.NyRevurdering::class, Toggle.NyTilstandsflyt::class)
internal class RevurderingV2E2ETest : AbstractEndToEndTest() {

    @Test
    fun `revurdere første periode`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(19.januar, Feriedag)))
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVSLUTTET)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
    }

    @Test
    fun `revurdere periode med forlengelse i avventer godkjenning`() {
        nyttVedtak(1.januar, 31.januar)
        forlengTilGodkjenning(1.februar, 28.februar)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
        assertForventetFeil(
            forklaring = "avventer uferdig støtter ikke gjenopptaBehandlingNy og ny tilstandsflyt forventer å bruke avventer tidligere eller overlappende perioder istedenfor avventer uferdig",
            nå = {
                assertTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_UFERDIG)
            },
            ønsket = {
                assertTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER, AVVENTER_HISTORIKK)
            }
        )
    }

    @Test
    fun `revurdere andre periode`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.februar, Feriedag)))
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVSLUTTET)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
    }

    @Test
    fun `revurdere tredje periode`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.mars, Feriedag)))
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
    }

    @Test
    fun `revurdere første to perioder`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Feriedag)))
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(1.februar, Feriedag)))

        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVSLUTTET)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
    }

    @Test
    fun `revurdere tildligere utbetaling`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(1.mars, 31.mars)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.januar, Feriedag)))
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
    }

    @Test
    fun `revurdere periode med nyere førstegangsbehandling innenfor samme agp`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(10.februar, 28.februar, arbeidsgiverperiode = listOf(1.januar til 16.januar))
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.januar, Feriedag)))
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
    }

    @Test
    fun `revurdere nyere skjæringstidspunkt så revurdere eldste`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(10.februar, 28.februar, arbeidsgiverperiode = listOf(1.januar til 16.januar))
        forlengVedtak(1.mars, 31.mars)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(15.februar, Feriedag)))
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(19.januar, Feriedag)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
    }

    @Test
    fun `revurdere eldre skjæringstidspunkt mens nyere revurdert skjæringstidspunkt utbetales`() {
        nyttVedtak(1.januar, 20.januar)
        forlengVedtak(21.januar, 31.januar)
        nyttVedtak(10.februar, 28.februar, arbeidsgiverperiode = listOf(1.januar til 16.januar))
        forlengVedtak(1.mars, 31.mars)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(15.februar, Feriedag)))

        håndterYtelser(4.vedtaksperiode)
        håndterSimulering(4.vedtaksperiode)
        håndterUtbetalingsgodkjenning(4.vedtaksperiode)

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(21.januar, Feriedag)))
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(19.januar, Feriedag)))

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertTilstander(4.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING)

        nullstillTilstandsendringer()
        håndterUtbetalt()
        assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_REVURDERING)
        assertTilstander(4.vedtaksperiode, TIL_UTBETALING, AVSLUTTET, AVVENTER_REVURDERING)

        nullstillTilstandsendringer()
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET)
        assertTilstander(3.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertTilstander(4.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
    }

    @Test
    fun `revurdering av eldre skjæringstidspunkt mens nyere skjæringstidspunkt utbetales`() {
        nyttVedtak(1.januar, 31.januar)
        tilGodkjent(1.mars, 31.mars, 100.prosent, førsteFraværsdag = 1.mars)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        assertTilstander(2.vedtaksperiode, TIL_UTBETALING)

        håndterUtbetalt()
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, TIL_UTBETALING, AVSLUTTET, AVVENTER_REVURDERING)
    }

    @Test
    fun `Periode med forlengelse - forlengelse blir revurdert, deretter revurdering på første`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.februar, Feriedag)))
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.januar, Feriedag)))
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
    }

    @Test
    fun `Periode med to forlengelser - forlengelse nummer en blir revurdert, deretter revurdering på første`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.februar, Feriedag)))
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.januar, Feriedag)))
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
    }

    @Test
    fun `periode med forlengelse etterfulgt av kort periode - kort periode avsluttes ikke før revurdering er ferdig - ny flyt`() {
        Toggle.NyTilstandsflyt.enable {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterYtelser(1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent))
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            nullstillTilstandsendringer()
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))

            håndterSykmelding(Sykmeldingsperiode(1.april, 16.april, 100.prosent))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.april, 16.april, 100.prosent))

            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
            assertForventetFeil(
                forklaring = "Vi vet ikke hva ønsket oppførsel egentlig bør være",
                nå = {
                    assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING)
                },
                ønsket = {
                    assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER)
                }
            )
        }
    }

    @Test
    fun `revurdere eldste skjæringstidspunkt så revurdere nyeste`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(10.februar, 28.februar, arbeidsgiverperiode = listOf(1.januar til 16.januar))
        forlengVedtak(1. mars, 31.mars)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(19.januar, Feriedag)))
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(15.februar, Feriedag)))
        assertTrue(inspektør.sykdomstidslinje[15.februar] is Dag.Feriedag)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
    }

    @Test
    fun `nyere revurdering til simulering, så revurdering på eldre`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(1.mars, 31.mars)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(20.mars, Feriedag)))
        håndterYtelser(2.vedtaksperiode)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(20.januar, Feriedag)))
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_REVURDERING)
    }

    @Test
    fun `nyere revurdering til godkjenning, så revurdering på eldre`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(1.mars, 31.mars)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(20.mars, Feriedag)))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(20.januar, Feriedag)))
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, AVVENTER_REVURDERING)
    }

    @Test
    fun `Periode med forlengelse - forlengelse blir revurdert og er til godkjenning, deretter revurdering på første`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.februar, Feriedag)))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.januar, Feriedag)))
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
    }

    @Test
    fun `periode med forlengelse etterfulgt av kort periode - kort periode avsluttes ikke før revurdering er ferdig`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))

        håndterSykmelding(Sykmeldingsperiode(1.april, 16.april, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.april, 16.april, 100.prosent))

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertForventetFeil(
            forklaring = "Vi vet ikke hva ønsket oppførsel egentlig bør være",
            nå = {
                assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING)
            },
            ønsket = {
                assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER)
            }
        )
    }

    @Test
    fun `revudering påvirkes ikke av gjenoppta behandling ved avsluttet uten utbetaling`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()
        nullstillTilstandsendringer()

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))

        håndterSykmelding(Sykmeldingsperiode(1.juni, 10.juni, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.juni, 10.juni, 100.prosent))
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertForventetFeil(
            nå = {
                assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING)
            },
            ønsket = {
                assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER)
            }
        )
    }

    @Test
    fun `out of order periode trigger revurdering`() {
        Toggle.RevurdereOutOfOrder.enable {
            nyttVedtak(1.mai, 31.mai)
            forlengVedtak(1.juni, 30.juni)
            nullstillTilstandsendringer()
            nyttVedtak(1.januar, 31.januar)
            assertSisteTilstand(3.vedtaksperiode, AVSLUTTET)
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        }
    }

    @Test
    fun `out of order periode uten utbetaling trigger revurdering`() {
        Toggle.RevurdereOutOfOrder.enable {
            nyttVedtak(1.mai, 31.mai)
            forlengVedtak(1.juni, 30.juni)
            nullstillTilstandsendringer()
            nyPeriode(1.januar til 15.januar)
            assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        }
    }

    @Test
    fun `out of order periode mens senere periode revurderes til utbetaling`() {
        Toggle.RevurdereOutOfOrder.enable {
            nyttVedtak(1.mai, 31.mai)
            forlengTilGodkjenning(1.juni, 30.juni)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)

            nyPeriode(1.januar til 31.januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            nullstillTilstandsendringer()

            assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, TIL_UTBETALING)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER)

            håndterUtbetalt()

            assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING)
            assertTilstander(3.vedtaksperiode, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER, AVVENTER_HISTORIKK)

            assertForventetFeil(
                forklaring = """Forventer at periode som går fra til utbetaling til avsluttet blir sendt videre til 
                    avventer revurdering dersom tidligere periode gjenopptar behandling""",
                nå = {
                    assertTilstander(2.vedtaksperiode, TIL_UTBETALING, AVSLUTTET)
                },
                ønsket = {
                    assertTilstander(2.vedtaksperiode, TIL_UTBETALING, AVSLUTTET, AVVENTER_REVURDERING)
                }
            )
        }
    }

    @Test
    fun `første periode i til utbetaling når det dukker opp en out of order-periode`() = Toggle.RevurdereOutOfOrder.enable {
        tilGodkjent(1.mars, 31.mars, 100.prosent, 1.mars)
        nyPeriode(1.januar til 31.januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        nullstillTilstandsendringer()

        assertTilstander(1.vedtaksperiode, TIL_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER)

        håndterUtbetalt()
        assertTilstander(2.vedtaksperiode, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER, AVVENTER_HISTORIKK)
        assertForventetFeil(
            forklaring = """Forventer at periode som går fra til utbetaling til avsluttet blir sendt videre til 
                    avventer revurdering dersom tidligere periode gjenopptar behandling""",
            nå = {
                assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET)
            },
            ønsket = {
                assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET, AVVENTER_REVURDERING)
            }
        )
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(2.vedtaksperiode, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER, AVVENTER_HISTORIKK, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET)
        assertForventetFeil(
            nå = {
                assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET)
            },
            ønsket = {
                assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
            }
        )
    }

    @Test
    fun `periode til utbetaling blir overstyrt`() {
        tilGodkjent(1.januar, 31.januar, 100.prosent, 1.januar)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))
        assertTilstander(1.vedtaksperiode, TIL_UTBETALING)
        håndterUtbetalt()
        assertForventetFeil(
            nå = {
                assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET)
            },
            ønsket = {
                assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
            }
        )
    }

    @Test
    fun `revurdert periode til utbetaling blir revurdert`() {
        nyttVedtak(1.januar, 31.januar)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)))

        assertTilstander(1.vedtaksperiode, TIL_UTBETALING)
        håndterUtbetalt()
        assertForventetFeil(
            nå = {
                assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET)
            },
            ønsket = {
                assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
            }
        )
    }
}