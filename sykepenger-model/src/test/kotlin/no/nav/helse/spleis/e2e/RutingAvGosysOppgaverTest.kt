package no.nav.helse.spleis.e2e

import no.nav.helse.ForventetFeil
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.*
import no.nav.helse.hendelser.til
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.somOrganisasjonsnummer
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
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
        assertTrue(observatør.opprettOppgaveForSpeilsaksbehandlereEvent().any { søknadHendelseId in it.hendelser })
    }

    @Test
    fun `søknad som er nære utbetalingsperioden avsluttet uten utbetaling skal ikke til ny kø `() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 15.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 15.februar, 100.prosent), Ferie(1.februar, 15.februar))
        håndterInntektsmelding(listOf(1.februar til 15.februar), førsteFraværsdag = 1.februar)

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
        håndterUtbetalingshistorikk(2.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 1.februar, 3.februar, 100.prosent, 2000.daglig), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER.toString(), 1.februar, 2000.daglig, true)
        ))
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
        assertTrue(observatør.opprettOppgaveForSpeilsaksbehandlereEvent().any { søknadHendelseId in it.hendelser })
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
        assertTrue(observatør.opprettOppgaveForSpeilsaksbehandlereEvent().any { søknadHendelseId in it.hendelser })
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
        nyttVedtak(1.mars, 31.mars, orgnummer = "999999999".somOrganisasjonsnummer())

        håndterSykmelding(Sykmeldingsperiode(20.februar, 12.mars, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(20.februar, 11.mars, 80.prosent))
        val søknadHendelseId = håndterSøknad(Sykdom(20.februar, 11.mars, 80.prosent))

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
        val søknadHendelseId = håndterSøknad(Sykdom(11.april, 19.april, 80.prosent), Papirsykmelding(11.april, 19.april))

        assertTrue(observatør.opprettOppgaveForSpeilsaksbehandlereEvent().isEmpty())
        assertTrue(observatør.opprettOppgaveEvent().any { søknadHendelseId in it.hendelser })
    }

    @Test
    fun `søknad som kommer inn i etterkant av en annullert utbetaling skal til vanlig kø`() {

        nyttVedtak(1.mars, 31.mars)
        håndterAnnullerUtbetaling()
        håndterSykmelding(Sykmeldingsperiode(11.april, 19.april, 80.prosent))
        val søknadHendelseId = håndterSøknad(Sykdom(11.april, 19.april, 80.prosent), Papirsykmelding(11.april, 19.april))

        assertTrue(observatør.opprettOppgaveForSpeilsaksbehandlereEvent().isEmpty())
        assertTrue(observatør.opprettOppgaveEvent().any { søknadHendelseId in it.hendelser })
    }

    @Test
    fun `arbeidsgiversøknad + inntektsmelding + søknad som blir forkastet`() {

        håndterSykmelding(Sykmeldingsperiode(1.januar, 10.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent))
        val inntektsmeldingId = håndterInntektsmelding(listOf(1.januar til 16.januar))

        håndterSykmelding(Sykmeldingsperiode(11.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(11.januar, 20.januar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, false)

        assertTrue(observatør.opprettOppgaveEvent().any { inntektsmeldingId in it.hendelser })
    }

    @ForventetFeil("Må sjekke inntektsmeldinger i forbindelse med forkasting. Løses i steg 3 - https://trello.com/c/yVDkucVG")
    @Test
    fun `inntektsmelding som kommer før periode som forkastes`() {
        val inntektsmeldingId = håndterInntektsmelding(listOf(1.januar til 16.januar))

        håndterSykmelding(Sykmeldingsperiode(1.januar, 10.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.januar, 11.januar, 100.prosent))

        assertTrue(observatør.opprettOppgaveEvent().any { inntektsmeldingId in it.hendelser })
    }

    @Test
    fun `overlappende sykmelding kaster ut perioden`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 10.januar, 100.prosent))
        val inntektsmeldingId = håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterSykmelding(Sykmeldingsperiode(2.januar, 11.januar, 100.prosent))

        assertTrue(observatør.opprettOppgaveEvent().any { inntektsmeldingId in it.hendelser })
    }

    @ForventetFeil("Inntektsmelding må sjekke mot forkastede. Løses i steg 2 - https://trello.com/c/yVDkucVG")
    @Test
    fun `inntektsmelding som treffer forkastet periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 10.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.januar, 11.januar, 100.prosent))

        val inntektsmeldingId = håndterInntektsmelding(listOf(1.januar til 16.januar))

        assertTrue(observatør.opprettOppgaveEvent().any { inntektsmeldingId in it.hendelser })
    }

    @Test
    fun `avvik i inntekt kaster ut perioden`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 10.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent))

        val inntektsmeldingId = håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 10000.månedlig)
        håndterSykmelding(Sykmeldingsperiode(11.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(11.januar, 20.januar, 100.prosent))

        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)

        assertTrue(observatør.opprettOppgaveEvent().any { inntektsmeldingId in it.hendelser })
    }

    @Test
    fun `søknad med ferie hele perioden + inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 17.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 17.januar, 100.prosent), Ferie(1.januar, 17.januar))
        val inntektsmeldingId = håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(18.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(18.januar, 20.januar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, false)

        assertTrue(observatør.opprettOppgaveEvent().any { inntektsmeldingId in it.hendelser })
    }

    @Test
    fun `søknad innenfor AGP + inntektsmelding + ny søknad som blir kasta ut`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 10.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent))
        val inntektsmeldingId = håndterInntektsmelding(listOf(1.januar til 16.januar))

        håndterSykmelding(Sykmeldingsperiode(11.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(11.januar, 20.januar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, false)

        assertTrue(observatør.opprettOppgaveEvent().any { inntektsmeldingId in it.hendelser })
    }

    @Test
    fun `søknad + inntektsmelding + ny søknad som blir kasta ut`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 17.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 17.januar, 100.prosent))
        val inntektsmeldingId = håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(18.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(18.januar, 20.januar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, false)

        assertFalse(observatør.opprettOppgaveEvent().any { inntektsmeldingId in it.hendelser })
    }

    @ForventetFeil("Inntektsmelding feilet fordi vi trenger søknad for å finne nærliggende utbetaling. Løses i steg 4 - https://trello.com/c/yVDkucVG")
    @Test
    fun `Inntektsmelding og forkasting uten søknad skal til egen kø`() {
        nyttVedtak(1.mars, 31.mars)

        håndterSykmelding(Sykmeldingsperiode(5.april, 25.mai, 100.prosent))
        val inntektsmeldingId = håndterInntektsmelding(listOf(5.april til 20.april))
        håndterSykmelding(Sykmeldingsperiode(5.april, 26.mai, 100.prosent))

        assertTrue(observatør.opprettOppgaveEvent().isEmpty())
        assertTrue(observatør.opprettOppgaveForSpeilsaksbehandlereEvent().any{inntektsmeldingId in it.hendelser})
    }
}
