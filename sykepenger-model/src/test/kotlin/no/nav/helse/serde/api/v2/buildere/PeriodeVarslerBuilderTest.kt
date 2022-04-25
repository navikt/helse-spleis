package no.nav.helse.serde.api.v2.buildere

import no.nav.helse.februar
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Utdanning
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.IdInnhenter
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.serde.api.AktivitetDTO
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class PeriodeVarslerBuilderTest: AbstractEndToEndTest() {

    @Test
    fun `varsel på samme skjæringstidspunkt kopieres`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar)) // Warning
        håndterYtelser()
        håndterVilkårsgrunnlag(1.vedtaksperiode, medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.VetIkke)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))

        assertTrue(aktiviteter(1.vedtaksperiode).any { it.alvorlighetsgrad == "W" && it.melding == "Vurder lovvalg og medlemskap" })
        assertEquals(0, aktiviteter(2.vedtaksperiode).size)
    }

    @Test
    fun `periode med varsel`(){
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 2.januar) // Warning
        håndterYtelser()
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser()
        håndterSimulering()

        assertEquals(1, aktiviteter(1.vedtaksperiode).size)
    }

    @Test
    fun `foregående uten utbetaling med warning og warning på periode to`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 15.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 2.januar) // Warning

        håndterSykmelding(Sykmeldingsperiode(16.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(16.januar, 31.januar, 100.prosent), Utdanning(30.januar, 31.januar))

        assertEquals(1, aktiviteter(1.vedtaksperiode).size)
        assertEquals(2, aktiviteter(2.vedtaksperiode).size)
    }

    @Test
    fun `plukker riktig vilkårsgrunnlag`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser()
        håndterVilkårsgrunnlag(1.vedtaksperiode, medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.VetIkke) // Warning på vilkårsgrunnlag
        håndterYtelser()
        håndterSimulering()
        håndterUtbetalingsgodkjenning()
        håndterUtbetalt()
        forlengVedtak(1.februar, 15.februar)

        nyttVedtak(4.mars, 31.mars)

        assertTrue(aktiviteter(1.vedtaksperiode).any { it.alvorlighetsgrad == "W" && it.melding == "Vurder lovvalg og medlemskap" })
        assertEquals(0, aktiviteter(2.vedtaksperiode).size)
        assertEquals(0, aktiviteter(3.vedtaksperiode).size)
    }

    private fun aktiviteter(vedtaksperiodeId: IdInnhenter): List<AktivitetDTO> {
        val vedtaksperiode = inspektør.vedtaksperioder(vedtaksperiodeId)
        val aktivitetsloggForegående = Vedtaksperiode.aktivitetsloggMedForegåendeUtenUtbetaling(vedtaksperiode)
        return PeriodeVarslerBuilder(aktivitetsloggForegående).build()
    }
}
