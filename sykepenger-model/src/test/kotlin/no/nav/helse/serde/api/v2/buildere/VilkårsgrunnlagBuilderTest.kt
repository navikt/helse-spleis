package no.nav.helse.serde.api.v2.buildere

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.assertForventetFeil
import no.nav.helse.desember
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsmelding.Refusjon
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.serde.api.dto.Arbeidsgiverinntekt
import no.nav.helse.serde.api.dto.InfotrygdVilkårsgrunnlag
import no.nav.helse.serde.api.dto.InntekterFraAOrdningen
import no.nav.helse.serde.api.dto.Inntektkilde
import no.nav.helse.serde.api.dto.SpleisVilkårsgrunnlag
import no.nav.helse.serde.api.dto.Vilkårsgrunnlag
import no.nav.helse.serde.api.dto.Vilkårsgrunnlagtype
import no.nav.helse.serde.api.speil.builders.InntektshistorikkForAOrdningenBuilder
import no.nav.helse.serde.api.speil.builders.OppsamletSammenligningsgrunnlagBuilder
import no.nav.helse.serde.api.speil.builders.VilkårsgrunnlagBuilder
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.finnSkjæringstidspunkt
import no.nav.helse.spleis.e2e.grunnlag
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrInntekt
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikk
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.lagInntektperioder
import no.nav.helse.spleis.e2e.nyeVedtak
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.spleis.e2e.repeat
import no.nav.helse.spleis.e2e.sammenligningsgrunnlag
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class VilkårsgrunnlagBuilderTest : AbstractEndToEndTest() {

    private companion object {
        private val AG1 = "987654321"
        private val AG2 = "123456789"
    }

    private val inntektshistorikkForAordningenBuilder get() = InntektshistorikkForAOrdningenBuilder(person)
    private val vilkårsgrunnlag get() = VilkårsgrunnlagBuilder(person, OppsamletSammenligningsgrunnlagBuilder(person), inntektshistorikkForAordningenBuilder)

    private val primitivInntekt = 40000.0
    private val inntekt = primitivInntekt.månedlig

    @Test
    fun `har en generasjon med vilkårsgrunnlag for periode til godkjenning`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = inntekt)
        håndterYtelser()
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntekt = inntekt)

        val vilkårsgrunnlagGenerasjoner = vilkårsgrunnlag.build().toDTO()
        val spleisgrunnlag = vilkårsgrunnlagGenerasjoner.første().vilkårsgrunnlagSpleis(1.januar)

        assertSpleisVilkårsprøving(
            vilkårsgrunnlag = spleisgrunnlag,
            sammenligningsgrunnlag = 480000.0,
            avviksprosent = 0.0,
            omregnetÅrsinntekt = 480000.0,
            skjæringstidspunkt = 1.januar,
            sykepengegrunnlag = 480000.0,
            oppfyllerKravOmMedlemskap = true
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
        assertSpleisVilkårsprøving(
            vilkårsgrunnlag = vilkårsgrunnlag,
            sammenligningsgrunnlag = 480000.0,
            avviksprosent = 0.0,
            omregnetÅrsinntekt = 480000.0,
            skjæringstidspunkt = 1.januar,
            sykepengegrunnlag = 480000.0,
            oppfyllerKravOmMedlemskap = true
        )

        assertEquals(2, vilkårsgrunnlag.inntekter.size)
        val inntektAg1 = vilkårsgrunnlag.inntekter.first { it.organisasjonsnummer == AG1 }
        assertEquals(228000.0, inntektAg1.sammenligningsgrunnlag)

        val inntektAg2 = vilkårsgrunnlag.inntekter.first { it.organisasjonsnummer == AG2 }
        assertEquals(252000.0, inntektAg2.sammenligningsgrunnlag)
    }


    @Test
    fun `revurdering av inntekt`() {
        nyttVedtak(1.januar, 31.januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 35000.månedlig, refusjon = Refusjon(35000.månedlig, null, emptyList()))
        håndterOverstyrInntekt(inntekt = 35000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser()
        håndterSimulering()

        val innslag = inspektør.vilkårsgrunnlagHistorikkInnslag()

        val generasjoner = vilkårsgrunnlag.build().toDTO()
        assertEquals(2, generasjoner.size)

        val førsteGenerasjon = requireNotNull(generasjoner[innslag.last().id]?.vilkårsgrunnlagSpleis(1.januar))
        assertSpleisVilkårsprøving(
            vilkårsgrunnlag = førsteGenerasjon,
            sammenligningsgrunnlag = 372000.0,
            avviksprosent = 0.0,
            omregnetÅrsinntekt = 372000.0,
            skjæringstidspunkt = 1.januar,
            sykepengegrunnlag = 372000.0,
            oppfyllerKravOmMedlemskap = true
        )
        assertEquals(1, førsteGenerasjon.inntekter.size)
        val inntekt = førsteGenerasjon.inntekter.first()
        assertEquals(372000.0, inntekt.sammenligningsgrunnlag)
        assertEquals(372000.0, inntekt.omregnetÅrsinntekt?.beløp)

        val andreGenerasjon = requireNotNull(generasjoner[innslag.first().id]?.vilkårsgrunnlagSpleis(1.januar))
        assertSpleisVilkårsprøving(
            vilkårsgrunnlag = andreGenerasjon,
            sammenligningsgrunnlag = 372000.0,
            avviksprosent = 12.9,
            omregnetÅrsinntekt = 420000.0,
            skjæringstidspunkt = 1.januar,
            sykepengegrunnlag = 420000.0,
            oppfyllerKravOmMedlemskap = true
        )
        assertEquals(1, andreGenerasjon.inntekter.size)
        val inntekt2 = andreGenerasjon.inntekter.first()
        assertEquals(372000.0, inntekt2.sammenligningsgrunnlag)
        assertEquals(420000.0, inntekt2.omregnetÅrsinntekt?.beløp)
    }

    @Test
    fun `revurdering av inntekt flere AG`() {
        nyeVedtak(1.januar, 31.januar, AG1, AG2) {
            lagInntektperioder(fom = 1.januar, inntekt = 19000.månedlig, orgnummer = AG1)
            lagInntektperioder(fom = 1.januar, inntekt = 21000.månedlig, orgnummer = AG2)
        }

        håndterOverstyrInntekt(inntekt = 18000.månedlig, skjæringstidspunkt = 1.januar, orgnummer = AG1)

        val innslag = inspektør.vilkårsgrunnlagHistorikkInnslag()
        val generasjoner = vilkårsgrunnlag.build().toDTO()

        val førsteGenerasjon = requireNotNull(generasjoner[innslag.last().id]?.vilkårsgrunnlagSpleis(1.januar))
        assertSpleisVilkårsprøving(
            vilkårsgrunnlag = førsteGenerasjon,
            sammenligningsgrunnlag = 480000.0,
            avviksprosent = 0.0,
            omregnetÅrsinntekt = 480000.0,
            skjæringstidspunkt = 1.januar,
            sykepengegrunnlag = 480000.0,
            oppfyllerKravOmMedlemskap = true
        )

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

        assertEquals(2, andreGenerasjon.inntekter.size)
        val inntekt2Ag1 = andreGenerasjon.inntekter.first { it.organisasjonsnummer == AG1 }
        val inntekt2Ag2 = andreGenerasjon.inntekter.first { it.organisasjonsnummer == AG2 }
        assertEquals(inntektAg2, inntekt2Ag2)

        assertForventetFeil(
            forklaring = "Vi støtter ikke revurdering av inntekt på flere arbeidsgivere. Vi overstyrer begge arbeidsgiverne",
            nå = {
                assertEquals(1, generasjoner.size)
                assertSpleisVilkårsprøving(
                    vilkårsgrunnlag = andreGenerasjon,
                    sammenligningsgrunnlag = 480000.0,
                    avviksprosent = 0.0,
                    omregnetÅrsinntekt = 480000.0,
                    skjæringstidspunkt = 1.januar,
                    sykepengegrunnlag = 480000.0,
                    oppfyllerKravOmMedlemskap = true
                )
                assertInntekt(
                    inntekt2Ag1,
                    sammenligningsgrunnlag = 228000.0,
                    orgnummer = AG1,
                    omregnetÅrsinntekt = 240000.0,
                    inntektskilde = Inntektkilde.Inntektsmelding,
                    omregnetÅrsinntektMånedsbeløp = 20000.0
                )
            },
            ønsket = {
                assertEquals(2, generasjoner.size)
                assertSpleisVilkårsprøving(
                    vilkårsgrunnlag = andreGenerasjon,
                    sammenligningsgrunnlag = 480000.0,
                    avviksprosent = 5.3,
                    omregnetÅrsinntekt = 456000.0,
                    skjæringstidspunkt = 1.januar,
                    sykepengegrunnlag = 456000.0,
                    oppfyllerKravOmMedlemskap = true
                )
                assertInntekt(
                    inntekt2Ag1,
                    sammenligningsgrunnlag = 228000.0,
                    orgnummer = AG1,
                    omregnetÅrsinntekt = 216000.0,
                    inntektskilde = Inntektkilde.Saksbehandler,
                    omregnetÅrsinntektMånedsbeløp = 18000.0
                )
            }
        )
    }

    @Test
    fun `flere skjæringstidspunkt`() {
        nyttVedtak(1.januar, 31.januar)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterInntektsmelding(listOf(1.mars til 16.mars))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.VetIkke)

        val generasjoner = vilkårsgrunnlag.build().toDTO()

        val førsteGenerasjon = generasjoner.første().vilkårsgrunnlagSpleis(1.januar)
        assertSpleisVilkårsprøving(
            vilkårsgrunnlag = førsteGenerasjon,
            sammenligningsgrunnlag = 372000.0,
            avviksprosent = 0.0,
            omregnetÅrsinntekt = 372000.0,
            skjæringstidspunkt = 1.januar,
            sykepengegrunnlag = 372000.0,
            oppfyllerKravOmMedlemskap = true
        )
        assertEquals(1, førsteGenerasjon.inntekter.size)
        val inntekt = førsteGenerasjon.inntekter.first()
        assertInntekt(inntekt, ORGNUMMER, 372000.0, 372000.0, Inntektkilde.Inntektsmelding, 31000.0)

        val andreGenerasjon = generasjoner.første().vilkårsgrunnlagSpleis(1.mars)
        assertSpleisVilkårsprøving(
            vilkårsgrunnlag = andreGenerasjon,
            sammenligningsgrunnlag = 372000.0,
            avviksprosent = 0.0,
            omregnetÅrsinntekt = 372000.0,
            skjæringstidspunkt = 1.mars,
            sykepengegrunnlag = 372000.0,
            oppfyllerKravOmMedlemskap = null
        )
        assertEquals(1, førsteGenerasjon.inntekter.size)
        val inntekt2 = førsteGenerasjon.inntekter.first()
        assertEquals(inntekt, inntekt2)
    }

    @Test
    fun `har med inntekt fra ghost`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = ORGNUMMER)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = ORGNUMMER)
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
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = inntekter, arbeidsforhold = emptyList()),
            arbeidsforhold = arbeidsforhold
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = AG1)
        håndterSimulering(1.vedtaksperiode, orgnummer = AG1)

        val generasjon = vilkårsgrunnlag.build().toDTO().første().vilkårsgrunnlagSpleis(1.januar)
        assertSpleisVilkårsprøving(
            vilkårsgrunnlag = generasjon,
            sammenligningsgrunnlag = 756000.0,
            avviksprosent = 0.0,
            omregnetÅrsinntekt = 756000.0,
            skjæringstidspunkt = 1.januar,
            sykepengegrunnlag = 561804.0,
            oppfyllerKravOmMedlemskap = true
        )
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
    ) {
        assertEquals(Vilkårsgrunnlagtype.INFOTRYGD, vilkårsgrunnlag.vilkårsgrunnlagtype)
        assertEquals(skjæringstidspunkt, vilkårsgrunnlag.skjæringstidspunkt)
        assertNull(vilkårsgrunnlag.sammenligningsgrunnlag)
        assertEquals(480000.0, vilkårsgrunnlag.omregnetÅrsinntekt)
    }

    private fun assertSpleisVilkårsprøving(
        vilkårsgrunnlag: SpleisVilkårsgrunnlag,
        sammenligningsgrunnlag: Double,
        avviksprosent: Double,
        omregnetÅrsinntekt: Double,
        skjæringstidspunkt: LocalDate,
        sykepengegrunnlag: Double,
        oppfyllerKravOmMedlemskap: Boolean?,
    ) {
        assertEquals(Vilkårsgrunnlagtype.SPLEIS, vilkårsgrunnlag.vilkårsgrunnlagtype)
        assertEquals(sammenligningsgrunnlag, vilkårsgrunnlag.sammenligningsgrunnlag)
        assertEquals(93634, vilkårsgrunnlag.grunnbeløp)
        assertEquals(true, vilkårsgrunnlag.oppfyllerKravOmMinstelønn)
        assertEquals(avviksprosent, BigDecimal(vilkårsgrunnlag.avviksprosent!!).setScale(2, RoundingMode.HALF_DOWN).toDouble())
        assertEquals(omregnetÅrsinntekt, vilkårsgrunnlag.omregnetÅrsinntekt)
        assertEquals(skjæringstidspunkt, vilkårsgrunnlag.skjæringstidspunkt)
        assertEquals(sykepengegrunnlag, vilkårsgrunnlag.sykepengegrunnlag)
        assertEquals(true, vilkårsgrunnlag.oppfyllerKravOmOpptjening)
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
