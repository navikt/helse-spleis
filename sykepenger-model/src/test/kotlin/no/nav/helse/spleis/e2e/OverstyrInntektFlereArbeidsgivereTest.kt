package no.nav.helse.spleis.e2e

import java.time.LocalDate
import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import no.nav.helse.assertForventetFeil
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.GrunnlagsdataInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.november
import no.nav.helse.person.ArbeidsgiverInntektsopplysning.Companion.inntektsopplysningPerArbeidsgiver
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EnableToggle(Toggle.RevurdereInntektMedFlereArbeidsgivere::class)
internal class OverstyrInntektFlereArbeidsgivereTest: AbstractEndToEndTest() {

    val grunnlagsdataInspektør get() = GrunnlagsdataInspektør(inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!)

    @Test
    fun `overstyr inntekt med flere AG -- happy case`() {
        tilGodkjenning(1.januar, 31.januar, a1, a2)
        assertTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)

        assertInntektForDato(20000.månedlig, 1.januar, inspektør = inspektør(a1))
        assertInntektForDato(20000.månedlig, 1.januar, inspektør = inspektør(a2))
        assertEquals(20000.månedlig, grunnlagsdataInspektør.sykepengegrunnlag.inspektør.arbeidsgiverInntektsopplysninger.inntektsopplysningPerArbeidsgiver()[a1]?.omregnetÅrsinntekt())
        assertEquals(20000.månedlig, grunnlagsdataInspektør.sykepengegrunnlag.inspektør.arbeidsgiverInntektsopplysninger.inntektsopplysningPerArbeidsgiver()[a2]?.omregnetÅrsinntekt())

        håndterOverstyrInntekt(19000.månedlig, a1, 1.januar)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)

        assertInntektForDato(19000.månedlig, 1.januar, inspektør = inspektør(a1))
        assertInntektForDato(20000.månedlig, 1.januar, inspektør = inspektør(a2))
        assertEquals(19000.månedlig, grunnlagsdataInspektør.sykepengegrunnlag.inspektør.arbeidsgiverInntektsopplysninger.inntektsopplysningPerArbeidsgiver()[a1]?.omregnetÅrsinntekt())
        assertEquals(20000.månedlig, grunnlagsdataInspektør.sykepengegrunnlag.inspektør.arbeidsgiverInntektsopplysninger.inntektsopplysningPerArbeidsgiver()[a2]?.omregnetÅrsinntekt())
    }

    @Test
    fun `overstyr inntekt med flere AG -- kan ikke overstyre perioden i AvventerBlokkerende`() {
        tilGodkjenning(1.januar, 31.januar, a1, a2)
        assertTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)

        håndterOverstyrInntekt(19000.månedlig, a2, 1.januar)
        assertNoErrors()
        assertInntektForDato(20000.månedlig, 1.januar, inspektør = inspektør(a1))
        assertInntektForDato(20000.månedlig, 1.januar, inspektør = inspektør(a2))
        assertEquals(20000.månedlig, grunnlagsdataInspektør.sykepengegrunnlag.inspektør.arbeidsgiverInntektsopplysninger.inntektsopplysningPerArbeidsgiver()[a1]?.omregnetÅrsinntekt())
        assertEquals(20000.månedlig, grunnlagsdataInspektør.sykepengegrunnlag.inspektør.arbeidsgiverInntektsopplysninger.inntektsopplysningPerArbeidsgiver()[a2]?.omregnetÅrsinntekt())
    }

    @Test
    fun `skal ikke kunne overstyre en arbeidsgiver hvis en annen er utbetalt`() {
        tilGodkjenning(1.januar, 31.januar, a1, a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterOverstyrInntekt(19000.månedlig, a2, 1.januar)
        assertForventetFeil(
            forklaring = "Dette burde støttes av at vi går inn i et revurderingsløp",
            nå = {
                assertTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
                assertTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING, orgnummer = a2)
                assertInntektForDato(20000.månedlig, 1.januar, inspektør = inspektør(a2))
            },
            ønsket = {
                assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a1)
                assertTilstand(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a2)
                assertInntektForDato(19000.månedlig, 1.januar, inspektør = inspektør(a2))
            }
        )
        assertInntektForDato(20000.månedlig, 1.januar, inspektør = inspektør(a1))
        assertEquals(20000.månedlig, grunnlagsdataInspektør.sykepengegrunnlag.inspektør.arbeidsgiverInntektsopplysninger.inntektsopplysningPerArbeidsgiver()[a1]?.omregnetÅrsinntekt())
    }

    @Test
    fun `flere arbeidsgivere med ghost - overstyrer inntekt til arbeidsgiver med sykdom -- happy case`() {
        tilOverstyring(
            sammenligningsgrunnlag = mapOf(a1 to 30000.månedlig, a2 to 1000.månedlig),
            sykepengegrunnlag = mapOf(a1 to 30000.månedlig, a2 to 1000.månedlig),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Vilkårsgrunnlag.Arbeidsforhold(a2, 1.november(2017), null)
            )
        )
        håndterOverstyrInntekt(29000.månedlig, a1, 1.januar)

        assertInntektForDato(29000.månedlig, 1.januar, inspektør = inspektør(a1))
        assertEquals(29000.månedlig, inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag?.inspektør?.arbeidsgiverInntektsopplysninger?.inntektsopplysningPerArbeidsgiver()?.get(a1)?.omregnetÅrsinntekt())

        nullstillTilstandsendringer()
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        assertTilstander(
            1.vedtaksperiode,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            orgnummer = a1
        )
    }

    @Test
    fun `overstyring av inntekt for flere arbeidsgivere som fører til 25% avvik skal gi error -- ghost`() {
        tilOverstyring(
            sammenligningsgrunnlag = mapOf(a1 to 30000.månedlig, a2 to 1000.månedlig),
            sykepengegrunnlag = mapOf(a1 to 30000.månedlig, a2 to 1000.månedlig),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Vilkårsgrunnlag.Arbeidsforhold(a2, 1.november(2017), null)
            )
        )
        håndterOverstyrInntekt(10000.månedlig, a1, 1.januar)

        assertInntektForDato(10000.månedlig, 1.januar, inspektør = inspektør(a1))
        assertEquals(10000.månedlig, inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag?.inspektør?.arbeidsgiverInntektsopplysninger?.inntektsopplysningPerArbeidsgiver()?.get(a1)?.omregnetÅrsinntekt())

        nullstillTilstandsendringer()
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        assertError("Har mer enn 25 % avvik", 1.vedtaksperiode.filter())
        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            AVVENTER_HISTORIKK,
            TIL_INFOTRYGD,
            orgnummer = a1
        )
    }

    @Test
    fun `overstyring av inntekt for flere arbeidsgivere som fører til 25% avvik skal gi error`() {
        tilGodkjenning(1.januar, 31.januar, a1, a2)
        håndterOverstyrInntekt(9999.månedlig, a1, 1.januar)

        assertInntektForDato(9999.månedlig, 1.januar, inspektør = inspektør(a1))
        assertEquals(9999.månedlig, inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag?.inspektør?.arbeidsgiverInntektsopplysninger?.inntektsopplysningPerArbeidsgiver()?.get(a1)?.omregnetÅrsinntekt())


        nullstillTilstandsendringer()
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        assertError("Har mer enn 25 % avvik", 1.vedtaksperiode.filter())
        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            AVVENTER_HISTORIKK,
            TIL_INFOTRYGD,
            orgnummer = a1
        )
        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            AVVENTER_BLOKKERENDE_PERIODE,
            TIL_INFOTRYGD,
            orgnummer = a2
        )
    }

    @Test
    fun `overstyrer inntekt til under krav til minste inntekt`() {
        tilGodkjenning(1.januar, 31.januar, a1, a2, beregnetInntekt = 1959.månedlig) {
            1.januar.minusYears(1) til 1.januar.minusMonths(1) inntekter {
                listOf(a1, a2).forEach {
                    it inntekt 1959.månedlig
                }
            }
        }
        håndterOverstyrInntekt(1500.månedlig, a1, 1.januar)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        assertWarning("Perioden er avslått på grunn av at inntekt er under krav til minste sykepengegrunnlag", 1.vedtaksperiode.filter(a1))
        assertNoErrors()
        assertTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
    }

    @Test
    fun `skal kaste ut vedtaksperioder dersom overstyring av inntekt kan føre til brukerutbetaling`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1, beregnetInntekt = INNTEKT/4)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a2, beregnetInntekt = INNTEKT/4)

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        val inntektsvurdering = Inntektsvurdering(
            listOf(
                sammenligningsgrunnlag(a1, 1.januar, (INNTEKT/4).repeat(12)),
                sammenligningsgrunnlag(a2, 1.januar, (INNTEKT/4).repeat(12))
            )
        )

        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a1, inntektsvurdering = inntektsvurdering)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterOverstyrInntekt(8000.månedlig, skjæringstidspunkt = 1.januar, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        assertError("Kan ikke fortsette på grunn av manglende funksjonalitet for utbetaling til bruker")
    }

    private fun tilOverstyring(
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        sammenligningsgrunnlag: Map<String, Inntekt>,
        sykepengegrunnlag: Map<String, Inntekt>,
        arbeidsforhold: List<Vilkårsgrunnlag.Arbeidsforhold>,
        beregnetInntekt: Inntekt = INNTEKT
    ) {
        håndterSykmelding(Sykmeldingsperiode(fom, tom, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(fom, tom, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(fom til fom.plusDays(15)), beregnetInntekt = beregnetInntekt, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        val inntektsvurdering = sammenligningsgrunnlag.keys.map { orgnummer ->
            sammenligningsgrunnlag(orgnummer, fom, sammenligningsgrunnlag[orgnummer]!!.repeat(12))
        }
        val inntektForSykepengegrunnlag = sykepengegrunnlag.keys.map { orgnummer ->
            grunnlag(orgnummer, fom, sykepengegrunnlag[orgnummer]!!.repeat(3))
        }
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(inntektsvurdering),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = inntektForSykepengegrunnlag, arbeidsforhold = emptyList()),
            arbeidsforhold = arbeidsforhold,
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
    }

}