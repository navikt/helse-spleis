package no.nav.helse.serde.api.v2.buildere

import no.nav.helse.Toggles
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Inntektsmelding.Refusjon
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.serde.api.v2.*
import no.nav.helse.serde.api.v2.Vilkårsgrunnlag
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

internal class VilkårsgrunnlagBuilderTest : AbstractEndToEndTest() {

    private companion object {
        private const val AG1 = "987654321"
        private const val AG2 = "123456789"
    }

    private val vilkårsgrunnlag get() = VilkårsgrunnlagBuilder(person, OppsamletSammenligningsgrunnlagBuilder(person))

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
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = inntekt)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterYtelser()
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntekt = inntekt)

        val vilkårsgrunnlagGenerasjoner = vilkårsgrunnlag.build().toDTO()
        val spleisgrunnlag = vilkårsgrunnlagGenerasjoner.første().vilkårsgrunnlagSpleis(1.januar)

        assertSpleisVilkårsprøving(
            spleisgrunnlag, 480000.0, 93634, true, 0.0, 480000.0, 1.januar, 480000.0, true, true
        )

        assertEquals(
            1, spleisgrunnlag.inntekter.size
        )

        val inntekt = spleisgrunnlag.inntekter.first()
        assertInntekt(inntekt, ORGNUMMER, 480000.0, 480000.0, Inntektkilde.Inntektsmelding, primitivInntekt)
    }

    @Test
    fun `har sammenligningsgrunnlag for alle arbeidsgivere`() {
        nyeVedtak(1.januar, 31.januar, AG1, AG2) {
            lagInntektperioder(fom = 1.januar, inntekt = 19000.månedlig, orgnummer = AG1)
            lagInntektperioder(fom = 1.januar, inntekt = 21000.månedlig, orgnummer = AG2)
        }

        val vilkårsgrunnlagGenerasjoner = vilkårsgrunnlag.build().toDTO()
        assertEquals(1, vilkårsgrunnlagGenerasjoner.size)

        val vilkårsgrunnlag = vilkårsgrunnlagGenerasjoner.første().vilkårsgrunnlagSpleis(1.januar)
        assertSpleisVilkårsprøving(vilkårsgrunnlag, 480000.0, 93634, true, 0.0, 480000.0, 1.januar, 480000.0, true, true)

        assertEquals(2, vilkårsgrunnlag.inntekter.size)
        val inntektAg1 = vilkårsgrunnlag.inntekter.first { it.organisasjonsnummer == AG1 }
        assertEquals(228000.0, inntektAg1.sammenligningsgrunnlag)

        val inntektAg2 = vilkårsgrunnlag.inntekter.first { it.organisasjonsnummer == AG2 }
        assertEquals(252000.0, inntektAg2.sammenligningsgrunnlag)
    }


    @Test
    fun `revurdering av inntekt`() {
        nyttVedtak(1.januar, 31.januar)
        håndterOverstyring(inntekt = 35000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser()
        håndterSimulering()

        val innslag = inspektør.vilkårsgrunnlagHistorikkInnslag()

        val generasjoner = vilkårsgrunnlag.build().toDTO()
        assertEquals(2, generasjoner.size)

        val førsteGenerasjon = requireNotNull(generasjoner[innslag.last().id]?.vilkårsgrunnlagSpleis(1.januar))
        assertSpleisVilkårsprøving(førsteGenerasjon, 372000.0, 93634, true, 0.0, 372000.0, 1.januar, 372000.0, true, true)
        assertEquals(1, førsteGenerasjon.inntekter.size)
        val inntekt = førsteGenerasjon.inntekter.first()
        assertEquals(372000.0, inntekt.sammenligningsgrunnlag)
        assertEquals(372000.0, inntekt.omregnetÅrsinntekt?.beløp)

        val andreGenerasjon = requireNotNull(generasjoner[innslag.first().id]?.vilkårsgrunnlagSpleis(1.januar))
        assertSpleisVilkårsprøving(andreGenerasjon, 372000.0, 93634, true, 12.9, 420000.0, 1.januar, 420000.0, true, true)
        assertEquals(1, andreGenerasjon.inntekter.size)
        val inntekt2 = andreGenerasjon.inntekter.first()
        assertEquals(372000.0, inntekt2.sammenligningsgrunnlag)
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
        val generasjoner = vilkårsgrunnlag.build().toDTO()
        assertEquals(2, generasjoner.size)

        val førsteGenerasjon = requireNotNull(generasjoner[innslag.last().id]?.vilkårsgrunnlagSpleis(1.januar))
        assertSpleisVilkårsprøving(førsteGenerasjon, 480000.0, 93634, true, 0.0, 480000.0, 1.januar, 480000.0, true, true)

        assertEquals(2, førsteGenerasjon.inntekter.size)
        val inntektAg1 = førsteGenerasjon.inntekter.first { it.organisasjonsnummer == AG1 }
        assertInntekt(
            inntektAg1,
            sammenligningsgrunnlag = 228000.0,
            orgnummer = AG1,
            omregnetÅrsinntekt = 240000.0,
            inntektskilde = Inntektkilde.Inntektsmelding,
            omregnetÅrsinntektMånedsbeløp = 20000.0
        )

        val inntektAg2 = førsteGenerasjon.inntekter.first { it.organisasjonsnummer == AG2 }
        assertInntekt(
            inntektAg2,
            sammenligningsgrunnlag = 252000.0,
            orgnummer = AG2,
            omregnetÅrsinntekt = 240000.0,
            inntektskilde = Inntektkilde.Inntektsmelding,
            omregnetÅrsinntektMånedsbeløp = 20000.0
        )

        val andreGenerasjon = requireNotNull(generasjoner[innslag.first().id]?.vilkårsgrunnlagSpleis(1.januar))
        assertSpleisVilkårsprøving(andreGenerasjon, 480000.0, 93634, true, 5.3, 456000.0, 1.januar, 456000.0, true, true)

        assertEquals(2, andreGenerasjon.inntekter.size)
        val inntekt2Ag1 = andreGenerasjon.inntekter.first { it.organisasjonsnummer == AG1 }
        assertInntekt(
            inntekt2Ag1,
            sammenligningsgrunnlag = 228000.0,
            orgnummer = AG1,
            omregnetÅrsinntekt = 216000.0,
            inntektskilde = Inntektkilde.Saksbehandler,
            omregnetÅrsinntektMånedsbeløp = 18000.0
        )

        val inntekt2Ag2 = andreGenerasjon.inntekter.first { it.organisasjonsnummer == AG2 }
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

        val generasjoner = vilkårsgrunnlag.build().toDTO()

        val førsteGenerasjon = generasjoner.første().vilkårsgrunnlagSpleis(1.januar)
        assertSpleisVilkårsprøving(førsteGenerasjon, 372000.0, 93634, true, 0.0, 372000.0, 1.januar, 372000.0, true, true)
        assertEquals(1, førsteGenerasjon.inntekter.size)
        val inntekt = førsteGenerasjon.inntekter.first()
        assertInntekt(inntekt, ORGNUMMER, 372000.0, 372000.0, Inntektkilde.Inntektsmelding, 31000.0)

        val andreGenerasjon = generasjoner.første().vilkårsgrunnlagSpleis(1.mars)
        assertSpleisVilkårsprøving(andreGenerasjon, 372000.0, 93634, true, 0.0, 372000.0, 1.mars, 372000.0, true, null)
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

        val generasjoner = vilkårsgrunnlag.build().toDTO()

        val førsteGenerasjon = generasjoner.første().vilkårsgrunnlagInfotrygd(skjæringstidspunkt)
        assertInfotrygdVilkårsprøving(førsteGenerasjon, skjæringstidspunkt, 480000.0)
        assertEquals(1, førsteGenerasjon.inntekter.size)
        val inntekt = førsteGenerasjon.inntekter.first()
        assertInntekt(inntekt, ORGNUMMER, null, 480000.0, Inntektkilde.Infotrygd, primitivInntekt)
    }

    @Test
    fun `har med inntekt fra ghost`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = ORGNUMMER)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = ORGNUMMER)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            refusjon = Refusjon(31000.månedlig, null, emptyList()),
            orgnummer = AG1
        )

        val inntekter = listOf(
            grunnlag(AG1, finnSkjæringstidspunkt(AG1, 1.vedtaksperiode), 31000.månedlig.repeat(3)),
            grunnlag(AG2, finnSkjæringstidspunkt(AG1, 1.vedtaksperiode), 32000.månedlig.repeat(3))
        )

        val arbeidsforhold = listOf(
            Arbeidsforhold(AG1, LocalDate.EPOCH, null),
            Arbeidsforhold(AG2, LocalDate.EPOCH, null)
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = AG1)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(AG1, finnSkjæringstidspunkt(AG1, 1.vedtaksperiode), 31000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(AG2, finnSkjæringstidspunkt(AG1, 1.vedtaksperiode), 32000.månedlig.repeat(12))
                )
            ),
            orgnummer = AG1,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter),
            arbeidsforhold = arbeidsforhold
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = AG1)
        håndterSimulering(1.vedtaksperiode, orgnummer = AG1)

        val generasjon = vilkårsgrunnlag.build().toDTO().første().vilkårsgrunnlagSpleis(1.januar)
        assertSpleisVilkårsprøving(generasjon, 756000.0, 93634, true, 0.0, 756000.0, 1.januar, 561804.0, true, true)
        assertEquals(2, generasjon.inntekter.size)
        val inntektAg1 = generasjon.inntekter.first { it.organisasjonsnummer == AG1 }
        assertInntekt(
            inntektAg1,
            sammenligningsgrunnlag = 372000.0,
            orgnummer = AG1,
            omregnetÅrsinntekt = 372000.0,
            inntektskilde = Inntektkilde.Inntektsmelding,
            omregnetÅrsinntektMånedsbeløp = 31000.0
        )
        val inntektGhost = generasjon.inntekter.first { it.organisasjonsnummer == AG2 }
        assertInntekt(
            inntektGhost,
            AG2, 384000.0, omregnetÅrsinntekt = 384000.0, Inntektkilde.AOrdningen, 32000.0, inntekterFraAOrdningen(1.januar, 32000.0)
        )
    }

    private fun assertInfotrygdVilkårsprøving(
        vilkårsgrunnlag: InfotrygdVilkårsgrunnlag,
        skjæringstidspunkt: LocalDate,
        omregnetÅrsinntekt: Double,
    ) {
        assertEquals(Vilkårsgrunnlagtype.INFOTRYGD, vilkårsgrunnlag.vilkårsgrunnlagtype)
        assertEquals(skjæringstidspunkt, vilkårsgrunnlag.skjæringstidspunkt)
        assertNull(vilkårsgrunnlag.sammenligningsgrunnlag)
        assertEquals(omregnetÅrsinntekt, vilkårsgrunnlag.omregnetÅrsinntekt)
    }

    private fun assertSpleisVilkårsprøving(
        vilkårsgrunnlag: SpleisVilkårsgrunnlag,
        sammenligningsgrunnlag: Double,
        grunnbeløp: Int,
        oppfyllerKravOmMinstelønn: Boolean,
        avviksprosent: Double,
        omregnetÅrsinntekt: Double,
        skjæringstidspunkt: LocalDate,
        sykepengegrunnlag: Double,
        oppfyllerKravOmOpptjening: Boolean,
        oppfyllerKravOmMedlemskap: Boolean?,
    ) {
        assertEquals(Vilkårsgrunnlagtype.SPLEIS, vilkårsgrunnlag.vilkårsgrunnlagtype)
        assertEquals(sammenligningsgrunnlag, vilkårsgrunnlag.sammenligningsgrunnlag)
        assertEquals(grunnbeløp, vilkårsgrunnlag.grunnbeløp)
        assertEquals(oppfyllerKravOmMinstelønn, vilkårsgrunnlag.oppfyllerKravOmMinstelønn)
        assertEquals(avviksprosent, BigDecimal(vilkårsgrunnlag.avviksprosent!!).setScale(2, RoundingMode.HALF_DOWN).toDouble())
        assertEquals(omregnetÅrsinntekt, vilkårsgrunnlag.omregnetÅrsinntekt)
        assertEquals(skjæringstidspunkt, vilkårsgrunnlag.skjæringstidspunkt)
        assertEquals(sykepengegrunnlag, vilkårsgrunnlag.sykepengegrunnlag)
        assertEquals(oppfyllerKravOmOpptjening, vilkårsgrunnlag.oppfyllerKravOmOpptjening)
        assertEquals(oppfyllerKravOmMedlemskap, vilkårsgrunnlag.oppfyllerKravOmMedlemskap)
    }

    private fun assertInntekt(
        inntekt: Arbeidsgiverinntekt,
        orgnummer: String,
        sammenligningsgrunnlag: Double?,
        omregnetÅrsinntekt: Double,
        inntektskilde: Inntektkilde,
        omregnetÅrsinntektMånedsbeløp: Double,
        inntektFraAOrdningen: List<InntekterFraAOrdningen>? = null
    ) {
        assertEquals(orgnummer, inntekt.organisasjonsnummer)
        assertEquals(sammenligningsgrunnlag, inntekt.sammenligningsgrunnlag)
        assertEquals(omregnetÅrsinntekt, inntekt.omregnetÅrsinntekt?.beløp)
        assertEquals(inntektskilde, inntekt.omregnetÅrsinntekt?.kilde)
        assertEquals(omregnetÅrsinntektMånedsbeløp, inntekt.omregnetÅrsinntekt?.månedsbeløp)
        assertEquals(inntektFraAOrdningen, inntekt.omregnetÅrsinntekt?.inntekterFraAOrdningen)
    }

    private fun Map<LocalDate, Vilkårsgrunnlag>.vilkårsgrunnlagSpleis(skjæringstidspunkt: LocalDate): SpleisVilkårsgrunnlag {
        return requireNotNull(this[skjæringstidspunkt] as SpleisVilkårsgrunnlag)
    }

    private fun Map<LocalDate, Vilkårsgrunnlag>.vilkårsgrunnlagInfotrygd(skjæringstidspunkt: LocalDate): InfotrygdVilkårsgrunnlag {
        return requireNotNull(this[skjæringstidspunkt] as InfotrygdVilkårsgrunnlag)
    }

    private fun Map<UUID, Map<LocalDate, Vilkårsgrunnlag>>.første() = this.values.first()

    private fun inntekterFraAOrdningen(skjæringstidspunkt: LocalDate, månedsbeløp: Double, antallMåneder: Int = 3): List<InntekterFraAOrdningen> {
        return (0 until antallMåneder).map {
            InntekterFraAOrdningen(
                måned = YearMonth.from(skjæringstidspunkt.minusMonths((antallMåneder - it).toLong())),
                sum = månedsbeløp
            )
        }
    }
}
