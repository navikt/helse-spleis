package no.nav.helse.serde.api.v2.buildere

import no.nav.helse.Toggles
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.serde.api.InntektsgrunnlagDTO
import no.nav.helse.serde.api.InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntektkildeDTO.*
import no.nav.helse.serde.api.MedlemskapstatusDTO
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.RoundingMode
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

        val andreGenerasjon = requireNotNull(generasjoner[innslag.first().id]?.vilkårsgrunnlagSpleis(1.januar))
        assertSpleisVilkårsprøving(andreGenerasjon, 372000.0, 93634, true, 12.9, MedlemskapstatusDTO.JA, 420000.0, 1.januar)
        assertEquals(1, andreGenerasjon.inntekter.size)
        val inntekt2 = andreGenerasjon.inntekter.first()
        assertEquals(372000.0, inntekt2.sammenligningsgrunnlag?.beløp)
        assertEquals(420000.0, inntekt2.omregnetÅrsinntekt?.beløp)
    }

    @Test
    @Disabled("Vi støtter ikke revurdering av inntekt på flere arbeidsgivere. Vi overstyrer begge arbeidsgiverne")
    fun `revurdering av inntekt flere AG`() {
        nyeVedtak(1.januar, 31.januar, AG1, AG2) {
            lagInntektperioder(fom = 1.januar, inntekt = 19000.månedlig, orgnummer = AG1)
            lagInntektperioder(fom = 1.januar, inntekt = 21000.månedlig, orgnummer = AG2)
        }

        håndterOverstyring(inntekt = 18000.månedlig, skjæringstidspunkt = 1.januar, orgnummer = AG1)

        val innslag = inspektør.vilkårsgrunnlagHistorikkInnslag()
        val generasjoner = vilkårsgrunnlag.generasjoner()
        assertEquals(2, generasjoner.size)

        val førsteGenerasjon = requireNotNull(generasjoner[innslag.last().id]?.vilkårsgrunnlagSpleis(1.januar))
        assertSpleisVilkårsprøving(førsteGenerasjon, 480000.0, 93634, true, 0.0, MedlemskapstatusDTO.JA, 480000.0, 1.januar)

        assertEquals(2, førsteGenerasjon.inntekter.size)
        val inntektAg1 = førsteGenerasjon.inntekter.first { it.arbeidsgiver == AG1 }
        assertArbeidsgiverinnekt(inntektAg1, sammenligningsgrunnlag = 228000.0, omregnetÅrsinntektBeløp = 240000.0, kilde = Inntektsmelding, månedsbeløp= 20000.0)

        val inntektAg2 = førsteGenerasjon.inntekter.first { it.arbeidsgiver == AG2 }
        assertArbeidsgiverinnekt(inntektAg2, sammenligningsgrunnlag = 252000.0, omregnetÅrsinntektBeløp = 240000.0, kilde = Inntektsmelding, månedsbeløp= 20000.0)

        val andreGenerasjon = requireNotNull(generasjoner[innslag.first().id]?.vilkårsgrunnlagSpleis(1.januar))
        assertSpleisVilkårsprøving(andreGenerasjon, 480000.0, 93634, true, 5.3, MedlemskapstatusDTO.JA, 456000.0, 1.januar)

        assertEquals(2, andreGenerasjon.inntekter.size)
        val inntekt2Ag1 = andreGenerasjon.inntekter.first { it.arbeidsgiver == AG1 }
        assertArbeidsgiverinnekt(inntekt2Ag1, sammenligningsgrunnlag = 228000.0, omregnetÅrsinntektBeløp = 216000.0, kilde = Saksbehandler, månedsbeløp= 18000.0)

        val inntekt2Ag2 = andreGenerasjon.inntekter.first { it.arbeidsgiver == AG2 }
        assertEquals(inntektAg2, inntekt2Ag2)
    }

    @Test
    fun `flere skjæringstidspunkt`() {
        nyttVedtak(1.januar, 31.januar)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterInntektsmelding(listOf(1.mars til 16.mars))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.mars, 31.mars, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.VetIkke)

        val generasjoner = vilkårsgrunnlag.generasjoner()

        val førsteGenerasjon = generasjoner.første().vilkårsgrunnlagSpleis(1.januar)
        assertSpleisVilkårsprøving(førsteGenerasjon, 372000.0, 93634, true, 0.0, MedlemskapstatusDTO.JA, 372000.0, 1.januar)
        assertEquals(1, førsteGenerasjon.inntekter.size)
        val inntekt = førsteGenerasjon.inntekter.first()
        assertInntekt(inntekt, ORGNUMMER, 372000.0, 372000.0, Inntektsmelding, 31000.0, false)

        val andreGenerasjon = generasjoner.første().vilkårsgrunnlagSpleis(1.mars)
        assertSpleisVilkårsprøving(andreGenerasjon, 372000.0, 93634, true, 0.0, MedlemskapstatusDTO.VET_IKKE, 372000.0, 1.mars)
        assertEquals(1, førsteGenerasjon.inntekter.size)
        val inntekt2 = førsteGenerasjon.inntekter.first()
        assertEquals(inntekt, inntekt2)
    }

    @Test
    fun `har ikke sammenligningsgrunnlag etter overgang fra Infotrygd`() {
        val skjæringstidspunkt = 1.desember(2017)
        val infotrygdperioder = arrayOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, skjæringstidspunkt, 31.desember(2017), 100.prosent, inntekt))
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, skjæringstidspunkt, inntekt, true))

        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, *infotrygdperioder, inntektshistorikk = inntektshistorikk)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        val generasjoner = vilkårsgrunnlag.generasjoner()

        val førsteGenerasjon = generasjoner.første().vilkårsgrunnlagInfotrygd(skjæringstidspunkt)
        assertInfotrygdVilkårsprøving(førsteGenerasjon, skjæringstidspunkt, 480000.0)
        assertEquals(1, førsteGenerasjon.inntekter.size)
        val inntekt = førsteGenerasjon.inntekter.first()
        assertInntekt(inntekt, ORGNUMMER, null, 480000.0, Infotrygd, primitivInntekt, false)
    }


    private fun assertInfotrygdVilkårsprøving(
        vilkårsgrunnlag: InfotrygdGrunnlag,
        skjæringstidspunkt: LocalDate,
        omregnetÅrsinntekt: Double,
    ) {
        assertEquals(skjæringstidspunkt, vilkårsgrunnlag.skjæringstidspunkt)
        assertNull(vilkårsgrunnlag.sammenligningsgrunnlag)
        assertEquals(omregnetÅrsinntekt, vilkårsgrunnlag.omregnetÅrsinntekt)
    }

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
        assertEquals(avviksprosent, BigDecimal(vilkårsgrunnlag.avviksprosent!!).setScale(2, RoundingMode.HALF_DOWN).toDouble())
        assertEquals(medlemskapstatus, vilkårsgrunnlag.medlemskapstatus)
        assertEquals(omregnetÅrsinntekt, vilkårsgrunnlag.omregnetÅrsinntekt)
        assertEquals(skjæringstidspunkt, vilkårsgrunnlag.skjæringstidspunkt)
    }

    private fun assertArbeidsgiverinnekt(
        inntekt: InntektsgrunnlagDTO.ArbeidsgiverinntektDTO,
        sammenligningsgrunnlag: Double,
        omregnetÅrsinntektBeløp: Double,
        kilde: InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntektkildeDTO,
        månedsbeløp: Double
    ) {
        assertEquals(sammenligningsgrunnlag, inntekt.sammenligningsgrunnlag?.beløp)
        assertEquals(omregnetÅrsinntektBeløp, inntekt.omregnetÅrsinntekt?.beløp)
        assertEquals(kilde, inntekt.omregnetÅrsinntekt?.kilde)
        assertEquals(månedsbeløp, inntekt.omregnetÅrsinntekt?.månedsbeløp)
    }

    private fun assertInntekt(
        inntekt: InntektsgrunnlagDTO.ArbeidsgiverinntektDTO,
        orgnummer: String,
        sammenligningsgrunnlag: Double?,
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
