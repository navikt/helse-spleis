package no.nav.helse.spleis.e2e

import no.nav.helse.ForventetFeil
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.oktober
import no.nav.helse.person.TilstandType.*
import no.nav.helse.september
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class OverstyrTidslinjeFlereAGTest : AbstractEndToEndTest() {
    @Test
    fun `kan ikke overstyre én AG hvis en annen AG har blitt godkjent`() {
        tilGodkjenning(1.januar, 31.januar, a1, a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterOverstyrTidslinje((29.januar til 29.januar).map { manuellFeriedag(it) }, orgnummer = a2)
        assertErrorTekst(inspektør(a2), "Kan ikke overstyre en pågående behandling der én eller flere perioder er behandlet ferdig")
    }

    @Test
    fun `overstyre en eldre periode hos en arbeidsgiver`() {
        nyttVedtak(1.januar, 31.januar, orgnummer = a1)
        tilGodkjenning(1.oktober, 30.oktober, a2)
        håndterOverstyrTidslinje((29.januar til 29.januar).map { manuellFeriedag(it) }, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_UFERDIG, a2)
    }

    @ForventetFeil("Dersom det er riktig at arbeidsgiver 2 påvirkes, skal uansett arbeidsgiver 2 få Gjennoppta behandling når arbeidsgiver 1 avsluttes")
    @Test
    fun `overstyre og utbetalte en eldre periode hos en arbeidsgiver`() {
        nyttVedtak(1.januar, 31.januar, orgnummer = a1)

        tilGodkjenning(1.oktober, 30.oktober, a2)

        håndterOverstyrTidslinje((29.januar til 29.januar).map { manuellFeriedag(it) }, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING, a2)
    }

    @ForventetFeil("skal gå til en ferdig tilstand når revurdert periode er avsluttet")
    @Test
    fun `gap-vedtaksperiode med sykmelding og søknad skal gå til en ferdig tilstand etter revurdert periode er avsluttet igjen`() {
        nyttVedtak(1.januar, 31.januar, orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.oktober, 30.oktober, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.oktober, 30.oktober, 100.prosent), orgnummer = a2)

        håndterOverstyrTidslinje((29.januar til 29.januar).map { manuellFeriedag(it) }, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, a1)
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_UFERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            orgnummer = a2
        )
    }

    @ForventetFeil("skal gå til en ferdig tilstand når revurdert periode er avsluttet")
    @Test
    fun `forlengelse med sykmelding og søknad skal gå til en ferdig tilstand etter revurdert periode er avsluttet igjen`() {
        nyttVedtak(1.januar, 31.januar, orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(15.september, 30.september, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(15.september, 30.september, 100.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(1.oktober, 30.oktober, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.oktober, 30.oktober, 100.prosent), orgnummer = a2)

        håndterOverstyrTidslinje((29.januar til 29.januar).map { manuellFeriedag(it) }, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, a1)
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
            AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE,
            orgnummer = a2
        )
    }

    @ForventetFeil("skal gå til en ferdig tilstand når revurdert periode er avsluttet")
    @Test
    fun `gap-vedtaksperiode med kun sykmelding skal gå til en ferdig tilstand etter revurdert periode er avsluttet igjen`() {
        nyttVedtak(1.januar, 31.januar, orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.oktober, 30.oktober, 100.prosent), orgnummer = a2)

        håndterOverstyrTidslinje((29.januar til 29.januar).map { manuellFeriedag(it) }, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, a1)
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            MOTTATT_SYKMELDING_UFERDIG_GAP,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            orgnummer = a2
        )
    }

    @ForventetFeil("skal gå til en ferdig tilstand når revurdert periode er avsluttet")
    @Test
    fun `forlengelse med kun sykmelding skal gå til en ferdig tilstand etter revurdert periode er avsluttet igjen`() {
        nyttVedtak(1.januar, 31.januar, orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(15.september, 30.september, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(15.september, 30.september, 100.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(1.oktober, 30.oktober, 100.prosent), orgnummer = a2)

        håndterOverstyrTidslinje((29.januar til 29.januar).map { manuellFeriedag(it) }, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, a1)
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            orgnummer = a2
        )
    }

    @ForventetFeil("skal gå til en ferdig tilstand når revurdert periode er avsluttet")
    @Test
    fun `gap-vedtaksperiode med sykmelding og inntektsmelding skal gå til en ferdig tilstand etter revurdert periode er avsluttet igjen`() {
        nyttVedtak(1.januar, 31.januar, orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.oktober, 30.oktober, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(listOf(1.oktober til 16.oktober), orgnummer = a2)

        håndterOverstyrTidslinje((29.januar til 29.januar).map { manuellFeriedag(it) }, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, a1)
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_SØKNAD_UFERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            orgnummer = a2
        )
    }

    @ForventetFeil("skal gå til en ferdig tilstand når revurdert periode er avsluttet")
    @Test
    fun `forlengelse med sykmelding og inntektsmelding skal gå til en ferdig tilstand etter revurdert periode er avsluttet igjen`() {
        nyttVedtak(1.januar, 31.januar, orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(15.september, 30.september, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(15.september, 30.september, 100.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(15.september, 30.september, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(15.september, 30.september, 100.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(1.oktober, 30.oktober, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(listOf(1.oktober til 16.oktober), orgnummer = a2)

        håndterOverstyrTidslinje((29.januar til 29.januar).map { manuellFeriedag(it) }, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = a1)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, a1)
        assertTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            AVVENTER_SØKNAD_FERDIG_FORLENGELSE,
            AVVENTER_SØKNAD_UFERDIG_FORLENGELSE,
            AVVENTER_SØKNAD_FERDIG_FORLENGELSE,
            orgnummer = a2
        )
    }
}
