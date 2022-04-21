package no.nav.helse.spleis.e2e

import java.util.UUID
import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EnableToggle(Toggle.NyTilstandsflyt::class)
internal class RutingAvInntektsmeldingoppgaverTest : AbstractEndToEndTest() {

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
}
