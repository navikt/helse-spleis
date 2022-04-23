package no.nav.helse.spleis.e2e

import java.util.UUID
import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EnableToggle(Toggle.NyTilstandsflyt::class)
internal class RutingAvInntektsmeldingOppgaverTest : AbstractEndToEndTest() {

    @Test
    fun `dersom vi mottar inntektsmelding før søknad skal det sendes et utsett_oppgave-event`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        val inntektsmeldingId = håndterInntektsmelding(listOf(1.januar til 16.januar))
        assertEquals(listOf(inntektsmeldingId), observatør.utsettOppgaveEventer().map { it.hendelse })
    }

    @Test
    fun `dersom inntektsmeldingen ikke treffer noen sykmeldinger skal det ikke sendes ut et utsett_oppgave-event`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.februar til 16.februar))
        assertEquals(emptyList<UUID>(), observatør.utsettOppgaveEventer().map { it.hendelse })
    }

    @Test
    fun `dersom inntektsmeldingen kommer etter søknad skal det ikke sendes ut et utsett_oppgave-event`() {
        // Utsettelse av oppgaveopprettelse blir da håndtert av vedtaksperiode_endret-event
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        assertEquals(emptyList<UUID>(), observatør.utsettOppgaveEventer().map { it.hendelse })
    }

    @Test
    fun `dersom vi mottar inntektsmeldingen før vi har mottatt noen andre dokumenter skal det ikke sendes ut et utsett_oppgave-event`() {
        håndterInntektsmelding(listOf(1.februar til 16.februar))
        assertEquals(emptyList<UUID>(), observatør.utsettOppgaveEventer().map { it.hendelse })
    }

    @Test
    fun `dersom vi mottar inntektsmelding før søknad og søknaden feiler skal det opprettes oppgave for både søknad og inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        val inntektsmeldingId = håndterInntektsmelding(listOf(1.januar til 16.januar))
        val søknadId = håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), andreInntektskilder = listOf(Søknad.Inntektskilde(true, "FRILANSER")))
        håndterInntektsmeldingReplay(inntektsmeldingId, 1.vedtaksperiode.id(ORGNUMMER))
        assertEquals(listOf(søknadId, inntektsmeldingId), observatør.opprettOppgaveEvent().flatMap { it.hendelser })
        assertEquals(listOf(1.vedtaksperiode.id(ORGNUMMER)), observatør.inntektsmeldingReplayEventer)
    }

    @Test
    fun `inntektsmelding replay av forkastede perioder skal kun be om replay for relevant vedtaksperiode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar(2017), 31.januar(2017), 100.prosent))
        val søknadId1 = håndterSøknad(Sykdom(1.januar(2017), 31.januar(2017), 100.prosent))
        val inntektsmeldingId1 = håndterInntektsmelding(listOf(1.januar(2017) til 16.januar(2017)))
        person.invaliderAllePerioder(hendelselogg, null)

        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        val inntektsmeldingId2 = håndterInntektsmelding(listOf(1.januar til 16.januar))
        val søknadId2 = håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), andreInntektskilder = listOf(Søknad.Inntektskilde(true, "FRILANSER")))
        håndterInntektsmeldingReplay(inntektsmeldingId1, 2.vedtaksperiode.id(ORGNUMMER))
        håndterInntektsmeldingReplay(inntektsmeldingId2, 2.vedtaksperiode.id(ORGNUMMER))
        assertEquals(listOf(søknadId1, inntektsmeldingId1, søknadId2, inntektsmeldingId2), observatør.opprettOppgaveEvent().flatMap { it.hendelser })
        assertEquals(listOf(1.vedtaksperiode.id(ORGNUMMER), 2.vedtaksperiode.id(ORGNUMMER)), observatør.inntektsmeldingReplayEventer)
    }

    @Test
    fun `dersom vi har en nærliggende utbetaling og vi mottar inntektsmelding før søknad og søknaden feiler - skal det opprettes oppgave i speilkøen i gosys`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterSykmelding(Sykmeldingsperiode(7.februar, 28.februar, 100.prosent))
        val inntektsmeldingId = håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 7.februar)
        val søknadId = håndterSøknad(Sykdom(7.februar, 28.februar, 100.prosent), andreInntektskilder = listOf(Søknad.Inntektskilde(true, "FRILANSER")))
        håndterInntektsmeldingReplay(inntektsmeldingId, 2.vedtaksperiode.id(ORGNUMMER))

        assertEquals(listOf(søknadId, inntektsmeldingId), observatør.opprettOppgaveForSpeilsaksbehandlereEvent().flatMap { it.hendelser })
    }
}
