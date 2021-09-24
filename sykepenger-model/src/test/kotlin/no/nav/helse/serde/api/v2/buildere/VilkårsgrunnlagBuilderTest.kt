package no.nav.helse.serde.api.v2.buildere

import no.nav.helse.Toggles
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.serde.api.InntektsgrunnlagDTO
import no.nav.helse.serde.api.InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntektkildeDTO.Inntektsmelding
import no.nav.helse.serde.api.MedlemskapstatusDTO
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class VilkårsgrunnlagBuilderTest : AbstractEndToEndTest() {

    private companion object {
        private const val AG1 = "987654321"
        private const val AG2 = "123456789"
    }

    private val vilkårsgrunnlag get() = VilkårsgrunnlagBuilder(person.vilkårsgrunnlagHistorikk, OppsamletSammenligningsgrunnlagBuilder(person))

    private val primitivInntekt = 40000.0
    private val inntekt = primitivInntekt.månedlig

    @BeforeEach
    fun before() {
        Toggles.RevurderInntekt.enable()
    }

    @AfterEach
    fun after() {
        Toggles.RevurderInntekt.disable()
    }

    @Test
    fun `har en generasjon med vilkårsgrunnlag for periode til godkjenning`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), refusjon = Refusjon(null, inntekt = inntekt))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterYtelser()
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntekt = inntekt)
        håndterYtelser()
        håndterSimulering()

        val vilkårsgrunnlagGenerasjoner = vilkårsgrunnlag.generasjoner()

        val spleisgrunnlag = vilkårsgrunnlagGenerasjoner.første().vilkårsgrunnlagSpleis(1.januar)

        assertSpleisVilkårsprøving(spleisgrunnlag, 480000.0, 93634, true, 0.0, MedlemskapstatusDTO.JA, 480000.0, 1.januar)

        assertEquals(1, spleisgrunnlag.inntekter.size)

        val inntekt = spleisgrunnlag.inntekter.first()
        assertInntekt(inntekt, ORGNUMMER, 480000.0, 480000.0, Inntektsmelding, primitivInntekt, false)
    }

    @Test
    fun `har sammenligningsgrunnlag for alle arbeidsgivere`() {
        nyeVedtak(1.januar, 31.januar, AG1, AG2) {
            lagInntektperioder(fom = 1.januar, inntekt = 19000.månedlig, orgnummer = AG1)
            lagInntektperioder(fom = 1.januar, inntekt = 21000.månedlig, orgnummer = AG2)
        }

        val vilkårsgrunnlagGenerasjoner = vilkårsgrunnlag.generasjoner()
        assertEquals(1, vilkårsgrunnlagGenerasjoner.size)

        val vilkårsgrunnlag = vilkårsgrunnlagGenerasjoner.første().vilkårsgrunnlagSpleis(1.januar)
        assertSpleisVilkårsprøving(vilkårsgrunnlag, 480000.0, 93634, true, 0.0, MedlemskapstatusDTO.JA, 480000.0, 1.januar)

        assertEquals(2, vilkårsgrunnlag.inntekter.size)
        val inntektAg1 = vilkårsgrunnlag.inntekter.first { it.arbeidsgiver == AG1 }
        assertEquals(228000.0, inntektAg1.sammenligningsgrunnlag?.beløp)

        val inntektAg2 = vilkårsgrunnlag.inntekter.first { it.arbeidsgiver == AG2 }
        assertEquals(252000.0, inntektAg2.sammenligningsgrunnlag?.beløp)
    }


    @Test
    fun `revurdering av inntekt`() {
        nyttVedtak(1.januar, 31.januar)
        håndterOverstyring(inntekt = 35000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser()
        håndterSimulering()

        val innslag = inspektør.vilkårsgrunnlagHistorikkInnslag()

        val generasjoner = vilkårsgrunnlag.generasjoner()
        assertEquals(2, generasjoner.size)

        val førsteGenerasjon = requireNotNull(generasjoner[innslag.last().id]?.vilkårsgrunnlagSpleis(1.januar))
        assertSpleisVilkårsprøving(førsteGenerasjon, 372000.0, 93634, true, 0.0, MedlemskapstatusDTO.JA, 372000.0, 1.januar)
        assertEquals(1, førsteGenerasjon.inntekter.size)
        val inntekt = førsteGenerasjon.inntekter.first()
        assertEquals(372000.0, inntekt.sammenligningsgrunnlag?.beløp)
        assertEquals(372000.0, inntekt.omregnetÅrsinntekt?.beløp)

        val andre = requireNotNull(generasjoner[innslag.first().id]?.vilkårsgrunnlagSpleis(1.januar))
        assertSpleisVilkårsprøving(førsteGenerasjon, 372000.0, 93634, true, 0.0, MedlemskapstatusDTO.JA, 372000.0, 1.januar)


    }

    @Test
    fun `revurdering av inntekt flere AG`() {
    }

    @Test
    fun `flere skjæringstidspunkt`() {}

    @Test
    fun `infotrygdforlengelse`() {}

//    @Test
//    fun `har ikke sammenligningsgrunnlag etter overgang fra Infotrygd`() {
//        val skjæringstidspunkt = 1.desember(2017)
//        val infotrygdperioder = arrayOf(ArbeidsgiverUtbetalingsperiode(AbstractPersonTest.ORGNUMMER, skjæringstidspunkt, 31.desember(2017), 100.prosent, inntekt))
//        val inntektshistorikk = listOf(Inntektsopplysning(AbstractPersonTest.ORGNUMMER, skjæringstidspunkt, inntekt, true))
//
//        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
//        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
//        håndterUtbetalingshistorikk(1.vedtaksperiode, *infotrygdperioder, inntektshistorikk = inntektshistorikk)
//        håndterYtelser(1.vedtaksperiode)
//        håndterSimulering(1.vedtaksperiode)
//
//        assertEquals(null, grunnlag.sammenligningsgrunnlag(AbstractPersonTest.ORGNUMMER, skjæringstidspunkt))
//    }
//

    private fun assertSpleisVilkårsprøving(
        vilkårsgrunnlag: SpleisGrunnlag,
        sammenligningsgrunnlag: Double,
        grunnbeløp: Int,
        oppfyllerKravOmMinstelønn: Boolean,
        avviksprosent: Double,
        medlemskapstatus: MedlemskapstatusDTO,
        omregnetÅrsinntekt: Double,
        skjæringstidspunkt: LocalDate
    ) {
        assertEquals(sammenligningsgrunnlag, vilkårsgrunnlag.sammenligningsgrunnlag)
        assertEquals(grunnbeløp, vilkårsgrunnlag.grunnbeløp)
        assertEquals(oppfyllerKravOmMinstelønn, vilkårsgrunnlag.oppfyllerKravOmMinstelønn)
        assertEquals(avviksprosent, vilkårsgrunnlag.avviksprosent)
        assertEquals(medlemskapstatus, vilkårsgrunnlag.medlemskapstatus)
        assertEquals(omregnetÅrsinntekt, vilkårsgrunnlag.omregnetÅrsinntekt)
        assertEquals(skjæringstidspunkt, vilkårsgrunnlag.skjæringstidspunkt)
    }

    private fun assertInntekt(
        inntekt: InntektsgrunnlagDTO.ArbeidsgiverinntektDTO,
        orgnummer: String,
        sammenligningsgrunnlag: Double,
        omregnetÅrsinntekt: Double,
        inntektskilde: InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntektkildeDTO,
        omregnetÅrsinntektMånedsbeløp: Double,
        harInntektFraAOrdningen: Boolean
    ) {
        assertEquals(orgnummer, inntekt.arbeidsgiver)
        assertEquals(sammenligningsgrunnlag, inntekt.sammenligningsgrunnlag?.beløp)
        assertEquals(omregnetÅrsinntekt, inntekt.omregnetÅrsinntekt?.beløp)
        assertEquals(inntektskilde, inntekt.omregnetÅrsinntekt?.kilde)
        assertEquals(omregnetÅrsinntektMånedsbeløp, inntekt.omregnetÅrsinntekt?.månedsbeløp)
        assertEquals(harInntektFraAOrdningen, inntekt.omregnetÅrsinntekt?.inntekterFraAOrdningen != null)
    }

    private fun IInnslag.vilkårsgrunnlagSpleis(skjæringstidspunkt: LocalDate): SpleisGrunnlag {
        return requireNotNull(this.vilkårsgrunnlag(skjæringstidspunkt) as SpleisGrunnlag)
    }

    private fun IInnslag.vilkårsgrunnlagInfotrygd(skjæringstidspunkt: LocalDate): InfotrygdGrunnlag {
        return requireNotNull(this.vilkårsgrunnlag(skjæringstidspunkt) as InfotrygdGrunnlag)
    }

    private fun Map<UUID, IInnslag>.første() = this.values.first()
}
