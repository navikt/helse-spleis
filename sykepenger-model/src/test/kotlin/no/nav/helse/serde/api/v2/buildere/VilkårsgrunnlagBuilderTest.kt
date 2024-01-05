package no.nav.helse.serde.api.v2.buildere

import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.Alder.Companion.alder
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsmelding.Refusjon
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.arbeidsgiver
import no.nav.helse.serde.api.dto.Arbeidsgiverinntekt
import no.nav.helse.serde.api.dto.InntekterFraAOrdningen
import no.nav.helse.serde.api.dto.Inntektkilde
import no.nav.helse.serde.api.dto.SpleisVilkårsgrunnlag
import no.nav.helse.serde.api.dto.Vilkårsgrunnlag
import no.nav.helse.serde.api.speil.builders.SpeilGenerasjonerBuilder
import no.nav.helse.serde.api.speil.builders.VilkårsgrunnlagBuilder
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.finnSkjæringstidspunkt
import no.nav.helse.spleis.e2e.grunnlag
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrInntekt
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.spleis.e2e.repeat
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VilkårsgrunnlagBuilderTest : AbstractEndToEndTest() {

    private companion object {
        private const val AG1 = "987654321"
        private const val AG2 = "123456789"
    }

    private fun vilkårsgrunnlag(organisasjonsnummer: String = AG1): Map<UUID, Vilkårsgrunnlag> {
        val vilkårsgrunnlagHistorikkBuilderResult = VilkårsgrunnlagBuilder(person.inspektør.vilkårsgrunnlagHistorikk).build()

        SpeilGenerasjonerBuilder(
            organisasjonsnummer,
            UNG_PERSON_FØDSELSDATO.alder,
            person.arbeidsgiver(organisasjonsnummer),
            vilkårsgrunnlagHistorikkBuilderResult
        ).build()
        return vilkårsgrunnlagHistorikkBuilderResult.toDTO()
    }

    private val primitivInntekt = 40000.0
    private val inntekt = primitivInntekt.månedlig

    @Test
    fun `har en generasjon med vilkårsgrunnlag for periode til godkjenning`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = inntekt,)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntekt = inntekt)
        håndterYtelser()
        håndterSimulering()

        val spleisgrunnlag = vilkårsgrunnlag().values.single() as SpleisVilkårsgrunnlag

        assertSpleisVilkårsprøving(
            vilkårsgrunnlag = spleisgrunnlag,
            omregnetÅrsinntekt = 480000.0,
            beregningsgrunnlag = 480000.0,
            skjæringstidspunkt = 1.januar,
            sykepengegrunnlag = 480000.0,
            oppfyllerKravOmMedlemskap = true
        )

        assertEquals(
            1, spleisgrunnlag.inntekter.size
        )

        val inntekt = spleisgrunnlag.inntekter.first()
        assertInntekt(inntekt, ORGNUMMER, 480000.0, Inntektkilde.Inntektsmelding, primitivInntekt)
    }

    @Test
    fun `revurdering av inntekt`() {
        nyttVedtak(1.januar, 31.januar)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar), beregnetInntekt = 35000.månedlig,
            refusjon = Refusjon(
                35000.månedlig,
                null,
                emptyList()
            ),
        )
        håndterOverstyrInntekt(inntekt = 35000.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser()
        håndterSimulering()

        val førsteGenerasjon = requireNotNull(vilkårsgrunnlag().values.first()) as SpleisVilkårsgrunnlag
        assertSpleisVilkårsprøving(
            vilkårsgrunnlag = førsteGenerasjon,
            omregnetÅrsinntekt = 372000.0,
            beregningsgrunnlag = 372000.0,
            skjæringstidspunkt = 1.januar,
            sykepengegrunnlag = 372000.0,
            oppfyllerKravOmMedlemskap = true,
        )
        assertEquals(1, førsteGenerasjon.inntekter.size)
        val inntekt = førsteGenerasjon.inntekter.first()
        assertEquals(372000.0, inntekt.omregnetÅrsinntekt.beløp)

        val andreGenerasjon = requireNotNull(vilkårsgrunnlag().values.last()) as SpleisVilkårsgrunnlag
        assertSpleisVilkårsprøving(
            vilkårsgrunnlag = andreGenerasjon,
            omregnetÅrsinntekt = 420000.0,
            beregningsgrunnlag = 420000.0,
            skjæringstidspunkt = 1.januar,
            sykepengegrunnlag = 420000.0,
            oppfyllerKravOmMedlemskap = true,
        )
        assertEquals(1, andreGenerasjon.inntekter.size)
        val inntekt2 = andreGenerasjon.inntekter.first()
        assertEquals(420000.0, inntekt2.omregnetÅrsinntekt.beløp)
    }

    @Test
    fun `flere skjæringstidspunkt`() {
        nyttVedtak(1.januar, 31.januar)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterInntektsmelding(listOf(1.mars til 16.mars),)
        håndterVilkårsgrunnlag(2.vedtaksperiode, medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.VetIkke)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)

        val førsteGenerasjon = requireNotNull(vilkårsgrunnlag().values.first()) as SpleisVilkårsgrunnlag
        assertSpleisVilkårsprøving(
            vilkårsgrunnlag = førsteGenerasjon,
            omregnetÅrsinntekt = 372000.0,
            beregningsgrunnlag = 372000.0,
            skjæringstidspunkt = 1.januar,
            sykepengegrunnlag = 372000.0,
            oppfyllerKravOmMedlemskap = true,
        )
        assertEquals(1, førsteGenerasjon.inntekter.size)
        val inntekt = førsteGenerasjon.inntekter.first()
        assertInntekt(inntekt, ORGNUMMER, 372000.0, Inntektkilde.Inntektsmelding, 31000.0)

        val andreGenerasjon = requireNotNull(vilkårsgrunnlag().values.last()) as SpleisVilkårsgrunnlag
        assertSpleisVilkårsprøving(
            vilkårsgrunnlag = andreGenerasjon,
            omregnetÅrsinntekt = 372000.0,
            beregningsgrunnlag = 372000.0,
            skjæringstidspunkt = 1.mars,
            sykepengegrunnlag = 372000.0,
            oppfyllerKravOmMedlemskap = null,
        )
        assertEquals(1, førsteGenerasjon.inntekter.size)
        val inntekt2 = førsteGenerasjon.inntekter.first()
        assertEquals(inntekt, inntekt2)
    }


    @Test
    fun `har med inntekt fra ghost`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = ORGNUMMER)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = ORGNUMMER)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            refusjon = Refusjon(31000.månedlig, null, emptyList()),
            orgnummer = AG1,
        )

        val inntekter = listOf(
            grunnlag(AG1, finnSkjæringstidspunkt(AG1, 1.vedtaksperiode), 31000.månedlig.repeat(3)),
            grunnlag(AG2, finnSkjæringstidspunkt(AG1, 1.vedtaksperiode), 32000.månedlig.repeat(3))
        )

        val arbeidsforhold = listOf(
            Arbeidsforhold(AG1, LocalDate.EPOCH, null, Arbeidsforhold.Arbeidsforholdtype.ORDINÆRT),
            Arbeidsforhold(AG2, LocalDate.EPOCH, null, Arbeidsforhold.Arbeidsforholdtype.ORDINÆRT)
        )

        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            orgnummer = AG1,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = inntekter, arbeidsforhold = emptyList()),
            arbeidsforhold = arbeidsforhold
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = AG1)
        håndterSimulering(1.vedtaksperiode, orgnummer = AG1)

        val generasjon = requireNotNull(vilkårsgrunnlag().values.single()) as SpleisVilkårsgrunnlag
        assertSpleisVilkårsprøving(
            vilkårsgrunnlag = generasjon,
            omregnetÅrsinntekt = 756000.0,
            beregningsgrunnlag = 756000.0,
            skjæringstidspunkt = 1.januar,
            sykepengegrunnlag = 561804.0,
            oppfyllerKravOmMedlemskap = true,
        )
        assertEquals(2, generasjon.inntekter.size)
        val inntektAg1 = generasjon.inntekter.first { it.organisasjonsnummer == AG1 }
        assertInntekt(
            inntektAg1,
            orgnummer = AG1,
            omregnetÅrsinntekt = 372000.0,
            inntektskilde = Inntektkilde.Inntektsmelding,
            omregnetÅrsinntektMånedsbeløp = 31000.0
        )
        val inntektGhost = generasjon.inntekter.first { it.organisasjonsnummer == AG2 }
        assertInntekt(
            inntektGhost,
            AG2,
            omregnetÅrsinntekt = 384000.0,
            Inntektkilde.AOrdningen,
            32000.0,
            inntekterFraAOrdningen(1.januar, 32000.0)
        )
    }

    private fun assertSpleisVilkårsprøving(
        vilkårsgrunnlag: SpleisVilkårsgrunnlag,
        omregnetÅrsinntekt: Double,
        beregningsgrunnlag: Double,
        skjæringstidspunkt: LocalDate,
        sykepengegrunnlag: Double,
        oppfyllerKravOmMedlemskap: Boolean?,
    ) {
        assertEquals(93634, vilkårsgrunnlag.grunnbeløp)
        assertEquals(true, vilkårsgrunnlag.oppfyllerKravOmMinstelønn)
        assertEquals(omregnetÅrsinntekt, vilkårsgrunnlag.omregnetÅrsinntekt)
        assertEquals(skjæringstidspunkt, vilkårsgrunnlag.skjæringstidspunkt)
        assertEquals(sykepengegrunnlag, vilkårsgrunnlag.sykepengegrunnlag)
        assertEquals(true, vilkårsgrunnlag.oppfyllerKravOmOpptjening)
        assertEquals(oppfyllerKravOmMedlemskap, vilkårsgrunnlag.oppfyllerKravOmMedlemskap)
        assertEquals(beregningsgrunnlag, vilkårsgrunnlag.beregningsgrunnlag)
    }

    private fun assertInntekt(
        inntekt: Arbeidsgiverinntekt,
        orgnummer: String,
        omregnetÅrsinntekt: Double,
        inntektskilde: Inntektkilde,
        omregnetÅrsinntektMånedsbeløp: Double,
        inntektFraAOrdningen: List<InntekterFraAOrdningen>? = null
    ) {
        assertEquals(orgnummer, inntekt.organisasjonsnummer)
        assertEquals(omregnetÅrsinntekt, inntekt.omregnetÅrsinntekt.beløp)
        assertEquals(inntektskilde, inntekt.omregnetÅrsinntekt.kilde)
        assertEquals(omregnetÅrsinntektMånedsbeløp, inntekt.omregnetÅrsinntekt.månedsbeløp)
        assertEquals(inntektFraAOrdningen, inntekt.omregnetÅrsinntekt.inntekterFraAOrdningen)
    }

    private fun inntekterFraAOrdningen(skjæringstidspunkt: LocalDate, månedsbeløp: Double, antallMåneder: Int = 3): List<InntekterFraAOrdningen> {
        return (0 until antallMåneder).map {
            InntekterFraAOrdningen(
                måned = YearMonth.from(skjæringstidspunkt.minusMonths((antallMåneder - it).toLong())),
                sum = månedsbeløp
            )
        }
    }
}
