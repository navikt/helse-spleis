package no.nav.helse.spleis.e2e

import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import no.nav.helse.desember
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

@EnableToggle(Toggle.NyTilstandsflyt::class)
internal class IkkeHåndtertHendelseTest : AbstractEndToEndTest() {
    @Test
    fun `håndterer hendelse_ikke_håndtert ved korrigerende søknad av utbetalt periode`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 26.januar, 100.prosent))
        håndterInntektsmelding(listOf(3.januar til 18.januar), førsteFraværsdag = 3.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        val søknadId = håndterSøknad(Sykdom(3.januar, 26.januar, 80.prosent))

        val hendelseIkkeHåndtert = observatør.hendelseIkkeHåndtert(søknadId)
        assertNotNull(hendelseIkkeHåndtert)
        assertEquals(
            listOf("Mottatt flere søknader for perioden - det støttes ikke før replay av hendelser er på plass"),
            hendelseIkkeHåndtert?.årsaker
        )
    }

    @Test
    fun `oppretter ikke sykmeldingsperiode dersom sykmelding er for gammel`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent), mottatt = 5.desember.atStartOfDay())
        assertEquals(0, observatør.hendelseIkkeHåndtertEventer.size)
        assertEquals(emptyList<Periode>(), inspektør.sykmeldingsperioder())
        assertError("Søknadsperioden kan ikke være eldre enn 6 måneder fra mottattidspunkt")
    }

    @Test
    fun `tar bare med errors som er relatert til hendelse`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 26.januar, 100.prosent))
        håndterInntektsmelding(listOf(3.januar til 18.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)

        person.håndter(ytelser(1.vedtaksperiode)) // for å legge på en feil som ikke skal være med i hendelse_ikke_håndtert
        håndterUtbetalt()

        val søknadId = håndterSøknad(Sykdom(3.januar, 25.januar, 80.prosent))

        val hendelseIkkeHåndtert = observatør.hendelseIkkeHåndtert(søknadId)
        assertNotNull(hendelseIkkeHåndtert)
        assertEquals(
            listOf("Mottatt flere søknader for perioden - det støttes ikke før replay av hendelser er på plass"),
            hendelseIkkeHåndtert?.årsaker
        )
    }
}