package no.nav.helse.serde.api

import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.serde.api.builders.InntektshistorikkForAOrdningenBuilder
import no.nav.helse.serde.api.v2.buildere.IInntekterFraAOrdningen
import no.nav.helse.serde.api.v2.buildere.IInntektkilde
import no.nav.helse.serde.api.v2.buildere.IOmregnetÅrsinntekt
import no.nav.helse.spleis.e2e.*
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth

internal class InntektshistorikkForAOrdningenBuilderTest : AbstractEndToEndTest() {
    @Test
    fun `henter sykdomsgrunnlaget for A-Ordningen`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = listOf(
                    grunnlag(a1, 1.januar, INNTEKT.repeat(3)),
                    grunnlag(a2, 1.januar, 6900.månedlig.repeat(3))

                ),
                arbeidsforhold = emptyList()
            ),
            inntektsvurdering = Inntektsvurdering(
                inntekter = listOf(
                    sammenligningsgrunnlag(a1, 1.januar, INNTEKT.repeat(12)),
                    sammenligningsgrunnlag(a2, 1.januar, 6900.månedlig.repeat(12))
                )
            ),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH)
            ),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.mars til 16.mars), beregnetInntekt = 35000.månedlig, orgnummer = a1)
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            2.vedtaksperiode,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = listOf(
                    grunnlag(a1, 1.mars, 35000.månedlig.repeat(3)),

                ),
                arbeidsforhold = emptyList()
            ),
            inntektsvurdering = Inntektsvurdering(
                inntekter = listOf(
                    sammenligningsgrunnlag(a1, 1.mars, 35000.månedlig.repeat(12)),
                )
            ),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH)
            ),
            orgnummer = a1
        )
        val builder = InntektshistorikkForAOrdningenBuilder(person)
        assertEquals(
            IOmregnetÅrsinntekt(
                kilde = IInntektkilde.AOrdningen,
                beløp = ÅRLIG_INNTEKT.toDouble(),
                månedsbeløp = MÅNEDLIG_INNTEKT.toDouble(),
                inntekterFraAOrdningen = listOf(
                    IInntekterFraAOrdningen(YearMonth.of(2017, Month.OCTOBER), MÅNEDLIG_INNTEKT.toDouble()),
                    IInntekterFraAOrdningen(YearMonth.of(2017, Month.NOVEMBER), MÅNEDLIG_INNTEKT.toDouble()),
                    IInntekterFraAOrdningen(YearMonth.of(2017, Month.DECEMBER), MÅNEDLIG_INNTEKT.toDouble())
                )
            ),
            builder.hentInntekt(a1, 1.januar)
        )
        assertEquals(
            IOmregnetÅrsinntekt(
                kilde = IInntektkilde.AOrdningen,
                beløp = 82800.0,
                månedsbeløp = 6900.0,
                inntekterFraAOrdningen = listOf(
                    IInntekterFraAOrdningen(YearMonth.of(2017, Month.OCTOBER), 6900.0),
                    IInntekterFraAOrdningen(YearMonth.of(2017, Month.NOVEMBER), 6900.0),
                    IInntekterFraAOrdningen(YearMonth.of(2017, Month.DECEMBER), 6900.0)
                )
            ),
            builder.hentInntekt(a2, 1.januar)
        )

        assertEquals(
            IOmregnetÅrsinntekt(
                kilde = IInntektkilde.AOrdningen,
                beløp = 420000.0,
                månedsbeløp = 35000.0,
                inntekterFraAOrdningen = listOf(
                    IInntekterFraAOrdningen(YearMonth.of(2017, Month.DECEMBER), 35000.0),
                    IInntekterFraAOrdningen(YearMonth.of(2018, Month.JANUARY), 35000.0),
                    IInntekterFraAOrdningen(YearMonth.of(2018, Month.FEBRUARY), 35000.0)
                )
            ),
            builder.hentInntekt(a1, 1.mars)
        )
    }
}
