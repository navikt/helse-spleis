package no.nav.helse.spleis.e2e.søknad

import no.nav.helse.assertForventetFeil
import no.nav.helse.februar
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Arbeid
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Permisjon
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Utdanning
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.søppelbøtte
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertIngenFunksjonelleFeil
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterInntektsmeldingMedValidering
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikk
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SøknadArbeidsgiverE2ETest : AbstractEndToEndTest() {

    @Test
    fun `avslutter søknad utenfor arbeidsgiverperioden dersom det kun er helg`() {
        håndterSykmelding(Sykmeldingsperiode(4.januar, 21.januar, 100.prosent))
        håndterSøknad(Sykdom(4.januar, 21.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `avslutter uferdig forlengelseperiode som bare strekkes inn i helg`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 5.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterSykmelding(Sykmeldingsperiode(6.januar, 19.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(20.januar, 21.januar, 100.prosent))
        håndterSøknad(Sykdom(6.januar, 19.januar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)
        håndterSøknad(Sykdom(20.januar, 21.januar, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode)
        assertTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        assertTilstand(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        håndterInntektsmelding(listOf(
            1.januar til 5.januar,
            6.januar til 9.januar, // lager et tredagers opphold (10. januar - 12. januar) som forskyver agp
            13.januar til 19.januar // til å slutte 19. januar. Periode nr 3. forlenger derfor kun helg, og skal også avsluttes uten utbetaling
        ), førsteFraværsdag = 13.januar)
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `avslutter uferdig forlengelseperiode som dekkes av arbeidsgiverperioden etter IM`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 5.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(6.januar, 19.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(20.januar, 23.januar, 100.prosent))
        håndterSøknad(Sykdom(6.januar, 19.januar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)
        håndterSøknad(Sykdom(20.januar, 23.januar, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode)

        assertTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        assertTilstand(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)

        håndterInntektsmelding(listOf(
            1.januar til 7.januar, // inntektsmeldingen oppgir nok opphold til at periode nr 3
            15.januar til 23.januar  // haver innenfor arbeidsgiverperioden likevel
        ), førsteFraværsdag = 15.januar)
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `avslutter ferdig forlengelseperiode som dekkes av arbeidsgiverperioden etter IM`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(17.januar, 21.januar, 100.prosent))
        håndterSøknad(Sykdom(17.januar, 21.januar, 100.prosent))
        håndterInntektsmelding(listOf(
            1.januar til 8.januar, // inntektsmeldingen oppgir nok opphold til at periode nr 2
            12.januar til 19.januar  // haver innenfor arbeidsgiverperioden likevel
        ), førsteFraværsdag = 12.januar)
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `starter med utdanning`() {
        håndterSykmelding(Sykmeldingsperiode(2.januar, 7.januar, 100.prosent))
        håndterSøknad(Sykdom(2.januar, 7.januar, 100.prosent), Utdanning(1.januar, 7.januar))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `litt permisjon`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 7.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 7.januar, 100.prosent), Permisjon(2.januar, 7.januar))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `bare ferie - kort periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 7.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 7.januar, 100.prosent), Ferie(1.januar, 7.januar))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `bare permisjon - kort periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 7.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 7.januar, 100.prosent), Permisjon(1.januar, 7.januar))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `bare ferie - lang periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(1.januar, 31.januar))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `bare ferie - forlengelser`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(1.januar, 31.januar))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)
        håndterInntektsmelding(listOf(1.januar til 16.januar))

        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
    }

    @Test
    fun `bare ferie - etter periode med bare ferie`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(1.januar, 31.januar))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Ferie(1.februar, 28.februar))
        håndterUtbetalingshistorikk(2.vedtaksperiode)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `bare ferie - etter tilbakevennende sykdom`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSykmelding(Sykmeldingsperiode(5.februar, 28.februar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 5.februar)
        håndterSøknad(Sykdom(5.februar, 28.februar, 100.prosent), Ferie(5.februar, 28.februar))

        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `bare permisjon - lang periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Permisjon(1.januar, 31.januar))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `starter med ferie`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 80.prosent))
        håndterSøknad(
            Sykdom(1.januar, 20.januar, 100.prosent),
            Ferie(1.januar, 3.januar),
            Ferie(18.januar, 19.januar)
        )
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `hensyntar forkastet historikk for å unngå å lage dårlig stemning - med gap til forkastet`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent))
        person.søppelbøtte(hendelselogg, 1.januar til 20.januar)
        håndterSykmelding(Sykmeldingsperiode(25.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(25.januar, 31.januar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
    }

    @Test
    fun `hensyntar forkastet historikk for å unngå å lage dårlig stemning - med mye gap til forkastet`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent))
        person.søppelbøtte(hendelselogg, 1.januar til 20.januar)
        håndterSykmelding(Sykmeldingsperiode(7.februar, 15.februar, 100.prosent))
        håndterSøknad(Sykdom(7.februar, 15.februar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `avslutter søknad innenfor arbeidsgiverperioden fordi arbeid er gjenopptatt`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), Arbeid(17.januar, 20.januar))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `avslutter søknad innenfor arbeidsgiverperioden dersom ferie er utenfor`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), Ferie(17.januar, 20.januar))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `hensyntar historikk fra infotrygd - får vite om det før IM`() {
        håndterSykmelding(Sykmeldingsperiode(3.februar, 18.februar, 100.prosent))
        håndterSøknad(Sykdom(3.februar, 18.februar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 31.januar, 100.prosent, INNTEKT), inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true)))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 3.februar)
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
    }

    @Test
    fun `hensyntar historikk fra infotrygd - får vite om det etter IM`() {
        håndterSykmelding(Sykmeldingsperiode(3.februar, 18.februar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 3.februar)
        håndterSøknad(Sykdom(3.februar, 18.februar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 31.januar, 100.prosent, INNTEKT), inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true)))
        håndterSøknad(Sykdom(3.februar, 18.februar, 100.prosent))
        assertForventetFeil(
            forklaring = "Vi får ikke inntektshistorikk før etter vi har kjørt replay av IM, så vi finner ikke ut at vi er en forlengelse. Dette kan bare skje om spleis aldri har fått vite om søknaden",
            nå = {
                assertTilstander(
                    1.vedtaksperiode,
                    START,
                    AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
                    AVVENTER_BLOKKERENDE_PERIODE,
                    AVSLUTTET_UTEN_UTBETALING
                )
            },
            ønsket = {
                assertTilstander(
                    1.vedtaksperiode,
                    START,
                    AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
                    AVVENTER_BLOKKERENDE_PERIODE,
                    AVVENTER_HISTORIKK
                )
            }
        )
    }

    @Test
    fun `hensyntar historikk fra infotrygd - får vite om det etter IM - flere perioder`() {
        håndterSykmelding(Sykmeldingsperiode(2.februar, 2.februar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 2.februar)
        håndterSøknad(Sykdom(2.februar, 2.februar, 100.prosent))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 31.januar, 100.prosent, INNTEKT),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true))
        )

        håndterSykmelding(Sykmeldingsperiode(6.februar, 6.februar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 6.februar)
        håndterSøknad(Sykdom(6.februar, 6.februar, 100.prosent))
        håndterUtbetalingshistorikk(
            2.vedtaksperiode,
            ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 31.januar, 100.prosent, INNTEKT),
            inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true))
        )

        assertForventetFeil(
            forklaring = "Vi får ikke inntektshistorikk før etter vi har kjørt replay av IM, så vi finner ikke ut at vi er en forlengelse. Dette kan bare skje om spleis aldri har fått vite om søknaden",
            nå = {
                assertTilstander(
                    1.vedtaksperiode,
                    START,
                    AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
                    AVVENTER_BLOKKERENDE_PERIODE,
                    AVSLUTTET_UTEN_UTBETALING
                )
                assertTilstander(
                    2.vedtaksperiode,
                    START,
                    AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
                    AVVENTER_BLOKKERENDE_PERIODE,
                    AVVENTER_HISTORIKK
                )
            },
            ønsket = {
                assertTilstander(
                    1.vedtaksperiode,
                    START,
                    AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
                    AVVENTER_HISTORIKK
                )
                assertTilstander(
                    2.vedtaksperiode,
                    START,
                    AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
                    AVVENTER_BLOKKERENDE_PERIODE
                )
            }
        )
    }

    @Test
    fun `avslutter korte perioder med gap`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 5.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(9.januar, 12.januar, 100.prosent))
        håndterSøknad(Sykdom(9.januar, 12.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(16.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(16.januar, 31.januar, 100.prosent))

        håndterInntektsmelding(listOf(
            1.januar til 5.januar,
            9.januar til 12.januar,
            16.januar til 22.januar
        ), førsteFraværsdag = 16.januar)
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
    }

    @Test
    fun `avslutter korte perioder med gap med arbeidsgiversøknad før IM`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(9.januar, 12.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(16.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 5.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterSøknad(Sykdom(9.januar, 12.januar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)
        håndterSøknad(Sykdom(16.januar, 31.januar, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode)
        håndterInntektsmelding(listOf(
            1.januar til 5.januar,
            9.januar til 12.januar,
            16.januar til 22.januar
        ), førsteFraværsdag = 16.januar)
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
    }

    @Test
    fun `avslutter korte perioder med forlengelse med arbeidsgiversøknad før IM`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(9.januar, 12.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(13.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 5.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterSøknad(Sykdom(9.januar, 12.januar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)
        håndterSøknad(Sykdom(13.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 5.januar, 9.januar til 19.januar), førsteFraværsdag = 9.januar)
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
    }

    @Test
    fun `avslutter korte perioder med forlengelse med arbeidsgiversøknad etter IM`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(9.januar, 12.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(13.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 5.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterSøknad(Sykdom(9.januar, 12.januar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)
        håndterSøknad(Sykdom(13.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(
            1.januar til 5.januar,
            9.januar til 19.januar
        ), førsteFraværsdag = 9.januar)
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
    }

    @Test
    fun `avslutter korte perioder med gap med søknad før IM`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(9.januar, 12.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(16.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 5.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterSøknad(Sykdom(9.januar, 12.januar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)
        håndterSøknad(Sykdom(16.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(
            1.januar til 5.januar,
            9.januar til 12.januar,
            16.januar til 22.januar
        ), førsteFraværsdag = 16.januar)
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
    }

    @Test
    fun `avslutter korte perioder med gap med søknad etter IM`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(9.januar, 12.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(16.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(
            1.januar til 5.januar,
            9.januar til 12.januar,
            16.januar til 22.januar
        ), førsteFraværsdag = 16.januar)
        håndterSøknad(Sykdom(1.januar, 5.januar, 100.prosent))
        håndterSøknad(Sykdom(9.januar, 12.januar, 100.prosent))
        håndterSøknad(Sykdom(16.januar, 31.januar, 100.prosent))
        assertEquals(1.januar til 5.januar, inspektør.periode(1.vedtaksperiode))
        assertEquals(9.januar til 12.januar, inspektør.periode(2.vedtaksperiode))
        assertEquals(16.januar til 31.januar, inspektør.periode(3.vedtaksperiode))
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
    }

    @Test
    fun `avslutter korte perioder med forlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 5.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(9.januar, 12.januar, 100.prosent))
        håndterSøknad(Sykdom(9.januar, 12.januar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(13.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(13.januar, 31.januar, 100.prosent))
        håndterUtbetalingshistorikk(3.vedtaksperiode)
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(
            1.januar til 5.januar,
            9.januar til 19.januar
        ), førsteFraværsdag = 9.januar)

        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
    }

    @Test
    fun `korrigerende søknad med gjenopptatt arbeid slik at hele perioden er innenfor arbeidsgiverperioden`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 18.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 18.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterSøknad(Sykdom(1.januar, 18.januar, 100.prosent), Arbeid(17.januar, 18.januar))
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertEquals(Utbetaling.Forkastet, inspektør.utbetalingtilstand(0))
        assertIngenFunksjonelleFeil(1.vedtaksperiode.filter())
    }

}
