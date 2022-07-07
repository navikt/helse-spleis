package no.nav.helse.spleis.e2e

import no.nav.helse.april
import no.nav.helse.assertForventetFeil
import no.nav.helse.februar
import no.nav.helse.hendelser.Inntektsmelding.Refusjon
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Papirsykmelding
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.søppelbøtte
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class RutingAvGosysOppgaverTest : AbstractEndToEndTest() {

    @Test
    fun `søknad som er nære utbetalingsperioden skal til ny kø`() {
        nyttVedtak(1.januar, 31.januar)

        håndterSykmelding(Sykmeldingsperiode(16.februar, 25.februar, 100.prosent))
        val søknadHendelseId = håndterSøknad(Sykdom(16.februar, 20.februar, 80.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), 16.februar)
        person.invaliderAllePerioder(hendelselogg, null)

        assertTrue(observatør.opprettOppgaveEvent().isEmpty())
        assertTrue(observatør.opprettOppgaveForSpeilsaksbehandlereEvent().any { søknadHendelseId in it.hendelser })
    }

    @Test
    fun `søknad som er nære utbetalingsperioden avsluttet uten utbetaling skal ikke til ny kø `() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 15.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 15.februar, 100.prosent), Ferie(1.februar, 15.februar))
        håndterInntektsmelding(listOf(1.februar til 15.februar), førsteFraværsdag = 1.februar)

        håndterSykmelding(Sykmeldingsperiode(16.februar, 25.februar, 100.prosent))

        val søknadHendelseId = håndterSøknad(Sykdom(16.februar, 20.februar, 80.prosent))
        person.invaliderAllePerioder(hendelselogg, null)

        assertTrue(observatør.opprettOppgaveForSpeilsaksbehandlereEvent().isEmpty())
        assertTrue(observatør.opprettOppgaveEvent().any { søknadHendelseId in it.hendelser })
    }

    @Test
    fun `søknad som har samme arbeidsgiverperiode som tidligere utbetaling`() {
        nyttVedtak(1.januar, 31.januar)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 3.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 3.februar, 100.prosent))
        person.invaliderAllePerioder(hendelselogg, null)

        håndterSykmelding(Sykmeldingsperiode(17.februar, 20.februar, 80.prosent))
        val søknadId = håndterSøknad(Sykdom(17.februar, 20.februar, 80.prosent))
        person.invaliderAllePerioder(hendelselogg, null)
        håndterUtbetalingshistorikk(2.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.februar, 3.februar, 100.prosent, 2000.daglig), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 1.februar, 2000.daglig, true)
        ))
        håndterInntektsmelding(listOf(1.januar til 16.januar), 17.februar)

        assertTrue(observatør.opprettOppgaveEvent().any { søknadId in it.hendelser })
        assertFalse(observatør.opprettOppgaveForSpeilsaksbehandlereEvent().any { søknadId in it.hendelser })
    }

    @Test
    fun `søknad som er lange nok etter utbetalingsperioden skal ikke til ny kø`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSykmelding(Sykmeldingsperiode(17.februar, 5.mars, 80.prosent))
        val søknadHendelseId = håndterSøknad(Sykdom(17.februar, 5.mars, 80.prosent))
        person.invaliderAllePerioder(hendelselogg, null)

        assertTrue(observatør.opprettOppgaveForSpeilsaksbehandlereEvent().isEmpty())
        assertTrue(observatør.opprettOppgaveEvent().any { søknadHendelseId in it.hendelser })
    }

    @Test
    fun `inntektsmelding som overlapper med utbetalt periode skal til ny kø`() {
        // Utbetaling 17. mars til 31. mars (AGP 1-16. mars)
        nyttVedtak(1.mars, 31.mars)
        val imHendelseId = håndterInntektsmelding(listOf(20.februar til 1.mars))
        assertForventetFeil(
            forklaring = "Nært forestående feature - https://trello.com/c/eG2Awz44",
            nå = { assertTrue(observatør.hendelseIkkeHåndtertEventer.isEmpty()) },
            ønsket = {
                // assertTrue(observatør.<nytt event her>.harRelatertUtbetaling)
                assertFalse(observatør.hendelseIkkeHåndtertEventer.isEmpty())
                assertNotNull(observatør.hendelseIkkeHåndtert(imHendelseId))
            }
        )
    }

    @Test
    fun `søknad 2 år før utbetalingsperioden - ny kø`() {
        nyttVedtak(1.mars, 31.mars)

        håndterSykmelding(Sykmeldingsperiode(1.februar(2016), 28.februar(2016), 80.prosent))
        val søknadHendelseId = håndterSøknad(Sykdom(1.februar(2016), 28.februar(2016), 80.prosent))

        assertTrue(observatør.opprettOppgaveEvent().isEmpty())
        assertTrue(observatør.opprettOppgaveForSpeilsaksbehandlereEvent().any { søknadHendelseId in it.hendelser })
    }

    @Test
    fun `søknad 4 år før utbetalingsperioden - vanlig kø`() {
        // Utbetaling 17. mars til 31. mars (AGP 1-16. mars)
        nyttVedtak(1.mars, 31.mars)

        håndterSykmelding(Sykmeldingsperiode(1.februar(2014), 28.februar(2014), 80.prosent))
        val søknadHendelseId = håndterSøknad(Sykdom(1.februar(2014), 28.februar(2014), 80.prosent))
        person.invaliderAllePerioder(hendelselogg, null)

        assertTrue(observatør.opprettOppgaveForSpeilsaksbehandlereEvent().isEmpty())
        assertTrue(observatør.opprettOppgaveEvent().any { søknadHendelseId in it.hendelser })
    }

    @Test
    fun `sjekker mot utbetalinger på tvers av arbeidsgivere`() {
        // Utbetaling 17. mars til 31. mars (AGP 1-16. mars)
        nyttVedtak(1.mars, 31.mars, orgnummer = "999999999")

        håndterSykmelding(Sykmeldingsperiode(20.februar, 11.mars, 80.prosent))
        val søknadHendelseId = håndterSøknad(Sykdom(20.februar, 11.mars, 80.prosent))

        håndterInntektsmelding(listOf(5.februar til 20.februar))
        person.invaliderAllePerioder(hendelselogg, null)

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

    @Test
    fun `overlappende sykmelding kaster ut perioden`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        val inntektsmeldingId = håndterInntektsmelding(listOf(1.januar til 16.januar))
        person.invaliderAllePerioder(hendelselogg, null)

        assertTrue(observatør.opprettOppgaveEvent().any { inntektsmeldingId in it.hendelser })
    }

    @Test
    fun `inntektsmelding som treffer forkastet periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))

        val inntektsmeldingId = håndterInntektsmelding(listOf(1.januar til 16.januar))
        person.invaliderAllePerioder(hendelselogg, null )

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
    fun `søknad med ferie hele perioden + inntektsmelding - ingen faktisk utbetaling, ergo ikke speil-relatert`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 17.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 17.januar, 100.prosent), Ferie(1.januar, 17.januar))
        val inntektsmeldingId = håndterInntektsmelding(listOf(1.januar til 16.januar))

        håndterSykmelding(Sykmeldingsperiode(18.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(18.januar, 20.januar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, false)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, TIL_INFOTRYGD)
        assertTrue(observatør.opprettOppgaveEvent().any { inntektsmeldingId in it.hendelser })
        assertTrue(observatør.opprettOppgaveForSpeilsaksbehandlereEvent().isEmpty())
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
        håndterUtbetalt()

        håndterSykmelding(Sykmeldingsperiode(18.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(18.januar, 20.januar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, false)

        assertFalse(observatør.opprettOppgaveEvent().any { inntektsmeldingId in it.hendelser })
    }

    @Test
    fun `Ingen eksplisitt oppgave for korrigert inntektsmelding, håndteres vha timeout`() {
        nyttVedtak(1.mars, 31.mars)

        håndterSykmelding(Sykmeldingsperiode(5.april, 25.mai, 100.prosent))
        val inntektsmeldingId = håndterInntektsmelding(listOf(1.mars til 16.mars), førsteFraværsdag = 1.mars)
        håndterSykmelding(Sykmeldingsperiode(5.april, 26.mai, 100.prosent))

        assertTrue(observatør.opprettOppgaveEvent().isEmpty())
        assertTrue(observatør.opprettOppgaveForSpeilsaksbehandlereEvent().none { inntektsmeldingId in it.hendelser })
    }

    @Test
    fun `Ferie teller likt som utbetaling når vi skal sjekke om vi har nærliggende utbetaling`() {
        nyttVedtak(1.mars, 31.mars)

        håndterSykmelding(Sykmeldingsperiode(1.april, 30.april, 100.prosent))
        håndterSøknad(Sykdom(1.april, 30.april, 100.prosent), Ferie(1.april, 30.april))
        håndterYtelser(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.mai, 26.mai, 100.prosent))
        håndterSøknad(Sykdom(1.mai, 26.mai, 100.prosent))
        val inntektsmeldingId = håndterInntektsmelding(listOf(1.mars til 16.mars), førsteFraværsdag = 1.mai) // NB - det er kanskje ikke realistisk at AG setter førsteFraværsdag etter ferie?
        person.invaliderAllePerioder(hendelselogg, null)

        assertNoWarning("Mottatt flere inntektsmeldinger - den første inntektsmeldingen som ble mottatt er lagt til grunn. Utbetal kun hvis det blir korrekt.", 1.vedtaksperiode.filter())
        assertNoWarning("Mottatt flere inntektsmeldinger - den første inntektsmeldingen som ble mottatt er lagt til grunn. Utbetal kun hvis det blir korrekt.", 2.vedtaksperiode.filter())
        assertWarning("Mottatt flere inntektsmeldinger - den første inntektsmeldingen som ble mottatt er lagt til grunn. Utbetal kun hvis det blir korrekt.", 3.vedtaksperiode.filter())
        assertTrue(observatør.opprettOppgaveEvent().isEmpty())
        assertTrue(observatør.opprettOppgaveForSpeilsaksbehandlereEvent().any { inntektsmeldingId in it.hendelser })
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(3.vedtaksperiode, TIL_INFOTRYGD)
    }

    @Test
    fun `periode med utbetaling_uten_utbetaling teller ikke som nærliggende utbetaling`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 21.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 21.januar, 100.prosent), Ferie(13.januar, 21.januar))
        val delvisRefusjon = Refusjon(321.årlig, null, emptyList())
        håndterInntektsmelding(listOf(1.januar til 16.januar), refusjon = delvisRefusjon)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

        håndterSykmelding(Sykmeldingsperiode(22.januar, 24.januar, 100.prosent))
        håndterSøknad(Sykdom(22.januar, 24.januar, 100.prosent))
        person.invaliderAllePerioder(hendelselogg, null)

        håndterSykmelding(Sykmeldingsperiode(22.januar, 22.januar, 100.prosent))
        val søknadId = håndterSøknad(Sykdom(22.januar, 22.januar, 100.prosent))

        assertTrue(observatør.opprettOppgaveEvent().any { søknadId in it.hendelser })
        assertFalse(observatør.opprettOppgaveForSpeilsaksbehandlereEvent().any { søknadId in it.hendelser })
    }

    @Test
    fun `forlengelse med infotrygd-utbetaling mellom`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.februar, 28.februar, 100.prosent, INNTEKT), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 1.februar, INNTEKT, true)
        ))
        val søknadId = håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        person.søppelbøtte(hendelselogg, 1.mars til 31.mars)
        assertTrue(observatør.opprettOppgaveEvent().any { søknadId in it.hendelser })
        assertFalse(observatør.opprettOppgaveForSpeilsaksbehandlereEvent().any { søknadId in it.hendelser })
    }

    @Test
    fun `førstegangbehandling med infotrygd-utbetaling mellom`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSykmelding(Sykmeldingsperiode(10.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(10.mars, 31.mars, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.februar, 28.februar, 100.prosent, INNTEKT), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 1.februar, INNTEKT, true)
        ))
        val søknadId = håndterSøknad(Sykdom(10.mars, 31.mars, 100.prosent))
        person.søppelbøtte(hendelselogg, 10.mars til 31.mars)
        assertTrue(observatør.opprettOppgaveEvent().any { søknadId in it.hendelser })
        assertFalse(observatør.opprettOppgaveForSpeilsaksbehandlereEvent().any { søknadId in it.hendelser })
    }

    @Test
    fun `førstegangbehandling med kort infotrygd-utbetaling mellom`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSykmelding(Sykmeldingsperiode(12.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(12.februar, 28.februar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.februar, 8.februar, 100.prosent, INNTEKT), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 1.februar, INNTEKT, true)
        ))
        val søknadId = håndterSøknad(Sykdom(12.februar, 28.februar, 100.prosent))
        person.søppelbøtte(hendelselogg, 12.februar til 28.februar)
        assertFalse(observatør.opprettOppgaveEvent().any { søknadId in it.hendelser })
        assertTrue(observatør.opprettOppgaveForSpeilsaksbehandlereEvent().any { søknadId in it.hendelser }) {
            "Selv om det er en Infotrygd-utbetaling mellom så er det ikke kritisk at vi likevel ruter oppgavene til Speil-benken," +
                "mht. at vi også har en utbetaling innenfor 16 dager fra perioden som kastes ut. En fremtidig utvikler har " +
                "likevel lov til å endre denne oppførselen :)"
        }
    }

    @Test
    fun `direkte overgang fra infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        val søknadId = håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 31.januar, 100.prosent, INNTEKT), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true)
        ))
        person.søppelbøtte(hendelselogg, 1.februar til 28.februar)
        assertTrue(observatør.opprettOppgaveEvent().any { søknadId in it.hendelser })
        assertFalse(observatør.opprettOppgaveForSpeilsaksbehandlereEvent().any { søknadId in it.hendelser })
    }
}
