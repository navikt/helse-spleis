package no.nav.helse.spleis.e2e

import java.time.LocalDate
import no.nav.helse.assertForventetFeil
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.november
import no.nav.helse.person.AbstractPersonTest
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class OverstyrGhostInntektTest : AbstractEndToEndTest() {

    private companion object {
        const val a1 = "987654321"
        const val a2 = "654321987"
    }

    @Test
    fun `Overstyrer ghost-inntekt -- happy case`() {
        tilOverstyring(
            sammenligningsgrunnlag = mapOf(a1 to 30000.månedlig, a2 to 1000.månedlig),
            sykepengegrunnlag = mapOf(a1 to 30000.månedlig, a2 to 1000.månedlig),
            arbeidsforhold = listOf(
                Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Arbeidsforhold(a2, 1.november(2017), null)
            )
        )
        håndterOverstyrInntekt(500.månedlig, a2, 1.januar)

        assertInntektForDato(500.månedlig, 1.januar, inspektør = inspektør(a2))
        assertEquals(500.månedlig, inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.sykepengegrunnlag()?.inntektsopplysningPerArbeidsgiver()?.get(a2)?.omregnetÅrsinntekt())
        assertEquals(1000.månedlig, inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.sammenligningsgrunnlagPerArbeidsgiver()?.get(a2)?.rapportertInntekt())

        nullstillTilstandsendringer()
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, orgnummer = a1)
    }

    @Test
    fun `Ved overstyring av inntekt til under krav til minste sykepengegrunnlag skal vi lage en utbetaling uten utbetaling`() {
        tilOverstyring(
            sammenligningsgrunnlag = mapOf(a1 to 3750.månedlig, a2 to 416.månedlig),
            sykepengegrunnlag = mapOf(a1 to 3750.månedlig, a2 to 416.månedlig),
            arbeidsforhold = listOf(
                Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Arbeidsforhold(a2, 1.november(2017), null)
            ),
            beregnetInntekt = 3750.månedlig
        )
        håndterOverstyrInntekt(INGEN, a2, 1.januar)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt()

        val utbetalinger = inspektør.utbetalinger
        assertEquals(2, utbetalinger.size)
        assertEquals(0, utbetalinger.last().inspektør.arbeidsgiverOppdrag.nettoBeløp())
        Assertions.assertTrue(utbetalinger.last().erAvsluttet())
        Assertions.assertTrue(utbetalinger.first().inspektør.erForkastet)

    }

    @Test
    fun `Overstyr ghost-inntekt -- ghost har ingen inntekt fra før av`() {
        tilOverstyring(
            sammenligningsgrunnlag = mapOf(a1 to 30000.månedlig),
            sykepengegrunnlag = mapOf(a1 to 30000.månedlig),
            arbeidsforhold = listOf(
                Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Arbeidsforhold(a2, 1.desember(2017), null)
            )
        )
        håndterOverstyrInntekt(500.månedlig, a2, 1.januar)

        assertInntektForDato(500.månedlig, 1.januar, inspektør = inspektør(a2))
        assertEquals(500.månedlig, inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.sykepengegrunnlag()?.inntektsopplysningPerArbeidsgiver()?.get(a2)?.omregnetÅrsinntekt())
        assertEquals(INGEN, inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.sammenligningsgrunnlagPerArbeidsgiver()?.get(a2)?.rapportertInntekt())

        nullstillTilstandsendringer()
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, orgnummer = a1)
    }

    @Test
    fun `Kan ikke overstyre ghost-inntekt for en forlengelse som allerede har tidligere utbetalinger`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode, arbeidsforhold = listOf(
                Arbeidsforhold(AbstractPersonTest.a1, LocalDate.EPOCH, null),
                Arbeidsforhold(AbstractPersonTest.a2, 1.desember(2017), null)
            )
        )
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, utbetalingGodkjent = true)
        håndterUtbetalt()
        // ny periode
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        val skjæringstidspunkt = inspektør.skjæringstidspunkt(2.vedtaksperiode)
        assertEquals(listOf(AbstractPersonTest.a1, AbstractPersonTest.a2).toList(), person.orgnummereMedRelevanteArbeidsforhold(skjæringstidspunkt).toList())
        assertThrows<Aktivitetslogg.AktivitetException> {
            håndterOverstyrInntekt(30000.månedlig, a2, skjæringstidspunkt)
        }
        assertSevere(
            "Kan ikke overstyre inntekt for ghost for en pågående behandling der én eller flere perioder er behandlet ferdig",
            AktivitetsloggFilter.person()
        )
        assertEquals(listOf(AbstractPersonTest.a1, AbstractPersonTest.a2), person.orgnummereMedRelevanteArbeidsforhold(skjæringstidspunkt))
    }

    @Test
    fun `overstyring av ghostinntekt som fører til 25 prosent avvik skal gi error`() {
        tilOverstyring(
            sammenligningsgrunnlag = mapOf(a1 to 30000.månedlig, a2 to 30000.månedlig),
            sykepengegrunnlag = mapOf(a1 to 30000.månedlig, a2 to 30000.månedlig),
            arbeidsforhold = listOf(
                Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Arbeidsforhold(a2, 1.november(2017), null)
            )
        )
        håndterOverstyrInntekt(500.månedlig, a2, 1.januar)

        assertInntektForDato(500.månedlig, 1.januar, inspektør = inspektør(a2))
        assertEquals(500.månedlig, inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.sykepengegrunnlag()?.inntektsopplysningPerArbeidsgiver()?.get(a2)?.omregnetÅrsinntekt())
        assertEquals(30000.månedlig, inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.sammenligningsgrunnlagPerArbeidsgiver()?.get(a2)?.rapportertInntekt())

        nullstillTilstandsendringer()
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        assertForkastetPeriodeTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK, TIL_INFOTRYGD, orgnummer = a1)

        assertForventetFeil(
            forklaring = "Burde ha enten warning eller error, https://trello.com/c/gk0F7acS",
            nå = {
                assertWarning("Har mer enn 25 % avvik. Dette støttes foreløpig ikke i Speil. Du må derfor annullere periodene.")
                assertError("Har mer enn 25 % avvik")
            },
            ønsket = {
                // Eller omvendt om det er det vi ønsker å gå for
                assertNoWarning("Har mer enn 25 % avvik. Dette støttes foreløpig ikke i Speil. Du må derfor annullere periodene.")
                assertError("Har mer enn 25 % avvik")
            }
        )
    }

    private fun tilOverstyring(
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        sammenligningsgrunnlag: Map<String, Inntekt>,
        sykepengegrunnlag: Map<String, Inntekt>,
        arbeidsforhold: List<Arbeidsforhold>,
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
