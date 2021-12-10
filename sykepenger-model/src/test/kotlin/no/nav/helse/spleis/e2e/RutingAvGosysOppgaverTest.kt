package no.nav.helse.spleis.e2e

import no.nav.helse.ForventetFeil
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.testhelpers.april
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class RutingAvGosysOppgaverTest : AbstractEndToEndTest() {

    @Test
    fun `søknad som er nære utbetalingsperioden skal til ny kø`() {
        nyttVedtak(1.januar, 31.januar)

        håndterSykmelding(Sykmeldingsperiode(16.februar, 25.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(16.februar, 20.februar, 80.prosent))
        // Søknad som ikke støttes pga dekker ikke 21- 25.februar
        val søknadHendelseId = håndterSøknad(Sykdom(16.februar, 20.februar, 80.prosent))
        // Inntektsmelding trigger sjekken av om søknad dekker hele perioden
        håndterInntektsmelding(listOf(1.januar til 16.januar), 16.februar)

        assertTrue(observatør.opprettOppgaveEvent().isEmpty())
        assertTrue(observatør.opprettOppgaveForSpeilsaksbehandlereEvent().any{søknadHendelseId in it.hendelser})
    }

    @Test
    fun `søknad som er nære utbetalingsperioden avsluttet uten utbetaling skal ikke til ny kø `() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 15.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 15.februar, 100.prosent), Søknad.Søknadsperiode.Ferie(1.februar, 15.februar))
        håndterInntektsmelding(listOf(1.februar til 15.februar), førsteFraværsdag = 1.februar)
        håndterYtelser()
        håndterVilkårsgrunnlag()
        håndterYtelser()

        håndterSykmelding(Sykmeldingsperiode(16.februar, 25.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(16.februar, 20.februar, 80.prosent))
        // Søknad som ikke støttes pga dekker ikke 21- 25.februar
        val søknadHendelseId = håndterSøknad(Sykdom(16.februar, 20.februar, 80.prosent))

        assertTrue(observatør.opprettOppgaveForSpeilsaksbehandlereEvent().isEmpty())
        assertTrue(observatør.opprettOppgaveEvent().any { søknadHendelseId in it.hendelser })
    }

    @Test
    fun `søknad som er lange nok etter utbetalingsperioden skal ikke til ny kø`() {
        nyttVedtak(1.januar, 31.januar)

        håndterSykmelding(Sykmeldingsperiode(17.februar, 25.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(17.februar, 20.februar, 80.prosent))
        val søknadHendelseId = håndterSøknad(Sykdom(17.februar, 20.februar, 80.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), 17.februar)

        assertTrue(observatør.opprettOppgaveForSpeilsaksbehandlereEvent().isEmpty())
        assertTrue(observatør.opprettOppgaveEvent().any { søknadHendelseId in it.hendelser })
    }

    @Test
    fun `ikke-håndtert søknad som ligger nær utbetalingsperioden skal til ny kø`() {
        // Utbetaling 17. mars til 31. mars (AGP 1-16. mars)
        nyttVedtak(1.mars, 31.mars)

        val søknadHendelseId = håndterSøknad(Sykdom(20.februar, 1.mars, 80.prosent))

        assertTrue(observatør.opprettOppgaveEvent().isEmpty())
        assertTrue(observatør.opprettOppgaveForSpeilsaksbehandlereEvent().any{søknadHendelseId in it.hendelser})
    }

    @Test
    @ForventetFeil("Nært forestående feature - https://trello.com/c/eG2Awz44")
    fun `inntektsmelding som overlapper med utbetalt periode skal til ny kø`() {
        // Utbetaling 17. mars til 31. mars (AGP 1-16. mars)
        nyttVedtak(1.mars, 31.mars)

        val imHendelseId = håndterInntektsmelding(listOf(20.februar til 1.mars))

        // assertTrue(observatør.<nytt event her>.harRelatertUtbetaling)
        assertNotNull(observatør.hendelseIkkeHåndtert(imHendelseId))
    }

    @Test
    fun `søknad 2 år før utbetalingsperioden - ny kø`() {
        nyttVedtak(1.mars, 31.mars)

        håndterSykmelding(Sykmeldingsperiode(20.februar(2016), 28.februar(2016), 80.prosent))
        val søknadHendelseId = håndterSøknad(Sykdom(20.februar(2016), 28.februar(2016), 80.prosent))

        assertTrue(observatør.opprettOppgaveEvent().isEmpty())
        assertTrue(observatør.opprettOppgaveForSpeilsaksbehandlereEvent().any{søknadHendelseId in it.hendelser})
    }

    @Test
    fun `søknad 4 år før utbetalingsperioden - vanlig kø`() {
        // Utbetaling 17. mars til 31. mars (AGP 1-16. mars)
        nyttVedtak(1.mars, 31.mars)

        håndterSykmelding(Sykmeldingsperiode(20.februar(2014), 28.februar(2014), 80.prosent))
        val søknadHendelseId = håndterSøknad(Sykdom(20.februar(2014), 28.februar(2014), 80.prosent))

        assertTrue(observatør.opprettOppgaveForSpeilsaksbehandlereEvent().isEmpty())
        assertTrue(observatør.opprettOppgaveEvent().any { søknadHendelseId in it.hendelser })
    }

    @Test
    fun `sjekker mot utbetalinger på tvers av arbeidsgivere`() {
        // Utbetaling 17. mars til 31. mars (AGP 1-16. mars)
        nyttVedtak(1.mars, 31.mars, orgnummer = "999999999")

        håndterSykmelding(Sykmeldingsperiode(20.februar, 2.mars, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(20.februar, 1.mars, 80.prosent))
        val søknadHendelseId = håndterSøknad(Sykdom(20.februar, 1.mars, 80.prosent))

        // kastes ut pga at søknaden ikke dekker hele vedtaksperioden
        håndterInntektsmelding(listOf(5.februar til 20.februar))

        assertTrue(observatør.opprettOppgaveEvent().isEmpty())
        assertTrue(observatør.opprettOppgaveForSpeilsaksbehandlereEvent().any { søknadHendelseId in it.hendelser })
    }

    @Test
    fun `søknad som kommer inn i etterkant av en avvist utbetaling skal til vanlig kø`() {

        tilGodkjenning(1.mars, 31.mars, ORGNUMMER)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, utbetalingGodkjent = false)
        håndterSykmelding(Sykmeldingsperiode(11.april, 19.april, 80.prosent))
        val søknadHendelseId = håndterSøknad(Sykdom(11.april, 19.april, 80.prosent), Søknad.Søknadsperiode.Papirsykmelding(11.april, 19.april))

        assertTrue(observatør.opprettOppgaveForSpeilsaksbehandlereEvent().isEmpty())
        assertTrue(observatør.opprettOppgaveEvent().any { søknadHendelseId in it.hendelser })

    }

    @Test
    fun `søknad som kommer inn i etterkant av en annullert utbetaling skal til vanlig kø`() {

        nyttVedtak(1.mars, 31.mars)
        håndterAnnullerUtbetaling()
        håndterSykmelding(Sykmeldingsperiode(11.april, 19.april, 80.prosent))
        val søknadHendelseId = håndterSøknad(Sykdom(11.april, 19.april, 80.prosent), Søknad.Søknadsperiode.Papirsykmelding(11.april, 19.april))

        assertTrue(observatør.opprettOppgaveForSpeilsaksbehandlereEvent().isEmpty())
        assertTrue(observatør.opprettOppgaveEvent().any { søknadHendelseId in it.hendelser })
    }
}


