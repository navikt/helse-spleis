package no.nav.helse.serde.api.v2.buildere

import no.nav.helse.februar
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Utdanning
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.IdInnhenter
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_2
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_MV_1
import no.nav.helse.serde.api.dto.AktivitetDTO
import no.nav.helse.serde.api.speil.builders.PeriodeVarslerBuilder
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertIngenVarsler
import no.nav.helse.spleis.e2e.assertVarsel
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
import org.junit.jupiter.api.Test

internal class PeriodeVarslerBuilderTest: AbstractEndToEndTest() {

    @Test
    fun `varsel på samme skjæringstidspunkt kopieres`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar)) // Warning
        håndterVilkårsgrunnlag(1.vedtaksperiode, medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.VetIkke)
        håndterYtelser()

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))

        assertVarsel(RV_MV_1, 1.vedtaksperiode.filter())
        assertEquals(0, aktiviteter(2.vedtaksperiode).size)
    }

    @Test
    fun `periode med varsel`(){
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 2.januar) // Warning
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser()
        håndterSimulering()

        assertVarsel(RV_IM_2, 1.vedtaksperiode.filter())
    }

    @Test
    fun `foregående uten utbetaling med warning og warning på periode to`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.januar))
        håndterSøknad(Sykdom(1.januar, 15.januar, 100.prosent), merknaderFraSykmelding = listOf(Søknad.Merknad("UGYLDIG_TILBAKEDATERING")))
        håndterInntektsmelding(listOf(1.januar til 16.januar))

        håndterSykmelding(Sykmeldingsperiode(16.januar, 31.januar))
        håndterSøknad(Sykdom(16.januar, 31.januar, 100.prosent), Utdanning(30.januar, 31.januar))

        assertEquals(1, aktiviteter(1.vedtaksperiode).size)
        assertEquals(2, aktiviteter(2.vedtaksperiode).size)
    }

    @Test
    fun `plukker riktig vilkårsgrunnlag`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode, medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.VetIkke) // Warning på vilkårsgrunnlag
        håndterYtelser()
        håndterSimulering()
        håndterUtbetalingsgodkjenning()
        håndterUtbetalt()
        forlengVedtak(1.februar, 15.februar)

        nyttVedtak(4.mars, 31.mars)

        assertVarsel(RV_MV_1, 1.vedtaksperiode.filter())
        assertIngenVarsler(2.vedtaksperiode.filter())
        assertIngenVarsler(3.vedtaksperiode.filter())
        assertEquals(0, aktiviteter(2.vedtaksperiode).size)
        assertEquals(0, aktiviteter(3.vedtaksperiode).size)
    }

    private fun aktiviteter(vedtaksperiodeId: IdInnhenter): List<AktivitetDTO> {
        val vedtaksperiode = inspektør.vedtaksperioder(vedtaksperiodeId)
        val aktivitetsloggForegående = Vedtaksperiode.aktivitetsloggMedForegåendeUtenUtbetaling(vedtaksperiode)
        return PeriodeVarslerBuilder(aktivitetsloggForegående).build()
    }
}
