package no.nav.helse.serde.api.v2.buildere

import no.nav.helse.hendelser.*
import no.nav.helse.inspectors.GrunnlagsdataInspektør
import no.nav.helse.person.IdInnhenter
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.serde.api.AktivitetDTO
import no.nav.helse.spleis.e2e.*
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

internal class PeriodeVarslerBuilderTest: AbstractEndToEndTest() {

    @Test
    fun `varsel på samme skjæringstidspunkt kopieres`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar)) // Warning
        håndterSøknad(SendtSøknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterYtelser()
        håndterVilkårsgrunnlag(1.vedtaksperiode, medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.VetIkke)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))

        assertEquals(1, aktiviteter(1.vedtaksperiode).size)
        assertEquals(1, aktiviteter(2.vedtaksperiode).size)
    }

    @Test
    fun `periode med varsel`(){
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 2.januar) // Warning
        håndterSøknad(SendtSøknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterYtelser()
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser()
        håndterSimulering()

        assertEquals(1, aktiviteter(1.vedtaksperiode).size)
    }

    @Test
    fun `foregående uten utbetaling med warning og warning på periode to`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(1.januar, 15.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 2.januar) // Warning

        håndterSykmelding(Sykmeldingsperiode(16.januar, 31.januar, 100.prosent))
        håndterSøknad(SendtSøknad.Søknadsperiode.Sykdom(16.januar, 31.januar, 100.prosent), SendtSøknad.Søknadsperiode.Utdanning(30.januar, 31.januar))

        assertEquals(1, aktiviteter(1.vedtaksperiode).size)
        assertEquals(2, aktiviteter(2.vedtaksperiode).size)
    }

    @Test
    fun `plukker riktig vilkårsgrunnlag`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterSøknad(SendtSøknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterYtelser()
        håndterVilkårsgrunnlag(1.vedtaksperiode, medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.VetIkke) // Warning på vilkårsgrunnlag
        håndterYtelser()
        håndterSimulering()
        håndterUtbetalingsgodkjenning()
        håndterUtbetalt()
        forlengVedtak(1.februar, 15.februar)

        nyttVedtak(1.mars, 31.mars)

        assertEquals(1, aktiviteter(1.vedtaksperiode).size)
        assertEquals(1, aktiviteter(2.vedtaksperiode).size)
        assertEquals(0, aktiviteter(3.vedtaksperiode).size)
    }

    private fun aktiviteter(vedtaksperiodeId: IdInnhenter): List<AktivitetDTO> {
        val vedtaksperiode = inspektør.vedtaksperioder(vedtaksperiodeId)
        val vilkårMeldingsreferanse = meldingsreferanseId(vedtaksperiodeId)
        val aktivitetsloggForegående = Vedtaksperiode.aktivitetsloggMedForegåendeUtenUtbetaling(vedtaksperiode)
        val aktivitetsloggVilkårsgrunnlag = Vedtaksperiode.hentVilkårsgrunnlagAktiviteter(vedtaksperiode)
        return VedtaksperiodeVarslerBuilder(vedtaksperiodeId(ORGNUMMER), aktivitetsloggForegående, aktivitetsloggVilkårsgrunnlag, vilkårMeldingsreferanse).build()
    }

    private fun meldingsreferanseId(vedtaksperiode: IdInnhenter): UUID? {
        return inspektør.vilkårsgrunnlag(vedtaksperiode)
            ?.let { it as? VilkårsgrunnlagHistorikk.Grunnlagsdata }
            ?.let { GrunnlagsdataInspektør(it) }
            ?.meldingsreferanseId
    }
}
