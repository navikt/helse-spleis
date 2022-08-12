package no.nav.helse.spleis.e2e

import no.nav.helse.assertForventetFeil
import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class InntektsmeldingOgFerieE2ETest : AbstractEndToEndTest() {

    @Test
    fun `ferie første dag i arbeidsgiverperioden`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(1.januar, 20.januar), Ferie(25.januar, 31.januar))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        assertForventetFeil(
            nå = {
                assertNoWarning("Sykmeldte har oppgitt ferie første dag i arbeidsgiverperioden.", 1.vedtaksperiode.filter(ORGNUMMER))
            },
            ønsket = {
                // TODO: https://trello.com/c/92DhehGa
                assertWarning("Sykmeldte har oppgitt ferie første dag i arbeidsgiverperioden.", 1.vedtaksperiode.filter(ORGNUMMER))
            }
        )
        assertWarning("Denne perioden var tidligere regnet som innenfor arbeidsgiverperioden", 1.vedtaksperiode.filter(ORGNUMMER))
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
    }

    @Test
    fun `ferieforlengelse`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 20.februar, 100.prosent))
        nullstillTilstandsendringer()
        håndterSøknad(Sykdom(1.februar, 20.februar, 100.prosent), Ferie(1.februar, 20.februar))

        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
    }

    @Test
    fun `ferie med gap til forrige, men samme skjæringstidspunkt`() {
        nyttVedtak(1.januar, 31.januar, orgnummer = a1)
        nullstillTilstandsendringer()
        håndterSykmelding(Sykmeldingsperiode(1.februar, 20.februar, 100.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(5.februar, 20.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(5.februar, 20.februar, 100.prosent), Ferie(5.februar, 20.februar), orgnummer = a1)
        håndterUtbetalingshistorikk(2.vedtaksperiode, orgnummer = a1)

        håndterSøknad(Sykdom(1.februar, 20.februar, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(listOf(1.februar til 16.februar), orgnummer = a2)

        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertForventetFeil(
            nå = {
                assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, orgnummer = a1)
                assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
            },
            ønsket = {
                assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVSLUTTET_UTEN_UTBETALING, orgnummer = a1)
                assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, orgnummer = a2)
            }
        )
    }

    @Test
    fun `ferie med gap til forrige, replay av IM`() {
        nyttVedtak(1.januar, 31.januar, orgnummer = a1)
        nullstillTilstandsendringer()
        håndterSykmelding(Sykmeldingsperiode(1.februar, 20.februar, 100.prosent), orgnummer = a2)

        val im = håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 5.februar, orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(5.februar, 20.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(5.februar, 20.februar, 100.prosent), Ferie(5.februar, 20.februar), orgnummer = a1)
        håndterInntektsmeldingReplay(im, 2.vedtaksperiode.id(a1))

        håndterSøknad(Sykdom(1.februar, 20.februar, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(listOf(1.februar til 16.februar), orgnummer = a2)

        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
    }

    @Test
    fun `bare ferie (forlengelse) - etter tilbakevennende sykdom`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSykmelding(Sykmeldingsperiode(5.februar, 23.februar, 100.prosent))
        val inntektsmeldingId = håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 5.februar)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(5.februar, 23.februar, 100.prosent), Ferie(5.februar, 23.februar))
        håndterSøknad(Sykdom(24.februar, 28.februar, 100.prosent), Ferie(24.februar, 28.februar))
        håndterInntektsmeldingReplay(inntektsmeldingId, 2.vedtaksperiode.id(ORGNUMMER))

        assertForventetFeil(
            nå = {
                assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
                assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE)
            },
            ønsket = {
                assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
                assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
            }
        )
    }

    @Test
    fun `bare ferie (sykdomsforlengelse) - etter tilbakevennende sykdom`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSykmelding(Sykmeldingsperiode(5.februar, 23.februar, 100.prosent))
        val inntektsmeldingId = håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 5.februar)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(5.februar, 23.februar, 100.prosent), Ferie(5.februar, 23.februar))
        håndterSøknad(Sykdom(24.februar, 28.februar, 100.prosent))
        håndterInntektsmeldingReplay(inntektsmeldingId, 2.vedtaksperiode.id(ORGNUMMER))

        assertForventetFeil(
            nå = {
                assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
                assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE)
            },
            ønsket = {
                assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
                assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
            }
        )
    }

}
