package no.nav.helse.spleis.e2e

import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

@EnableToggle(Toggle.NyTilstandsflyt::class)
internal class RutingAvSøknadOppgaverTest : AbstractEndToEndTest() {

    @Test
    fun `dersom vi har en nærliggende utbetaling og vi mottar overlappende søknader - skal det opprettes oppgave i speilkøen i gosys`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterSykmelding(Sykmeldingsperiode(20.januar, 10.februar, 100.prosent))
        val søknadId = håndterSøknad(Sykdom(20.januar, 10.februar, 100.prosent))

        Assertions.assertEquals(
            listOf(søknadId),
            observatør.opprettOppgaveForSpeilsaksbehandlereEvent().flatMap { it.hendelser })
    }

    @Test
    fun `dersom vi IKKE har en nærliggende utbetaling og vi mottar overlappende søknader - skal det opprettes oppgave i den vanlige gosyskøen`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        val søknadId1 = håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(20.januar, 10.februar, 100.prosent))
        val søknadId2 = håndterSøknad(Sykdom(20.januar, 10.februar, 100.prosent))
        /*
        søknadId2 kommer med i to opprett_oppgave-eventer her fordi første gang blir den kalt fra
        TilInfotrygd#entering(), deretter fra Arbeidsgiver#opprettVedtaksperiodeOgHåndter(), fordi det ikke blir
        opprettet en vedtaksperiode for søknaden
        */
        Assertions.assertEquals(
            listOf(søknadId1, søknadId2, søknadId2),
            observatør.opprettOppgaveEvent().flatMap { it.hendelser }
        )
    }

    @Test
    fun `dersom vi IKKE har en nærliggende utbetaling og vi mottar søknad som overlapper med periode i AUU - skal det opprettes oppgave i den vanlige gosyskøen`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(14.januar, 10.februar, 100.prosent))
        val søknadId2 = håndterSøknad(Sykdom(14.januar, 10.februar, 100.prosent))
        /*
        Her blir ikke 1.vedtaksperiode forkastet fordi den står i AvsluttetUtenUtbetaling, derfor sender vi kun ut et
        opprett_oppgave-event fra Arbeidsgiver#opprettVedtaksperiodeOgHåndter()
        */
        Assertions.assertEquals(
            listOf(søknadId2),
            observatør.opprettOppgaveEvent().flatMap { it.hendelser }
        )
    }

}
