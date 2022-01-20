package no.nav.helse.spleis.e2e

import no.nav.helse.ForventetFeil
import no.nav.helse.hendelser.*
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class OverstyrArbeidsforholdTest : AbstractEndToEndTest() {
    @Test
    fun `fjerner arbeidsforhold fra arbeidsforholdhistorikken ved overstyring`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode, arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1.toString(), LocalDate.EPOCH, null),
                Vilkårsgrunnlag.Arbeidsforhold(a2.toString(), 1.desember(2017), null)
            )
        )
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        val skjæringstidspunkt = inspektør.skjæringstidspunkt(1.vedtaksperiode)
        assertEquals(listOf(a1.toString(), a2.toString()).toList(), person.orgnummereMedRelevanteArbeidsforhold(skjæringstidspunkt).toList())
        håndterOverstyrArbeidsforhold(skjæringstidspunkt, listOf(OverstyrArbeidsforhold.ArbeidsforholdOverstyrt(a2, false)))
        assertEquals(listOf(a1.toString()), person.orgnummereMedRelevanteArbeidsforhold(skjæringstidspunkt))
    }

    @ForventetFeil("Dette gjør vi etter lunsj")
    @Test
    fun `Overstyring av arbeidsforhold fører til et nytt vilkårsgrunnlag med nye inntektsopplysninger`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode, arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1.toString(), LocalDate.EPOCH, null),
                Vilkårsgrunnlag.Arbeidsforhold(a2.toString(), 1.desember(2017), null)
            )
        )
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        val skjæringstidspunkt = inspektør.skjæringstidspunkt(1.vedtaksperiode)
        håndterOverstyrArbeidsforhold(skjæringstidspunkt, listOf(OverstyrArbeidsforhold.ArbeidsforholdOverstyrt(a2, false)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        val vilkårsgrunnlag = person.vilkårsgrunnlagFor(skjæringstidspunkt)
        assertEquals(setOf(a1.toString()), vilkårsgrunnlag?.inntektsopplysningPerArbeidsgiver()?.keys)
    }

    @ForventetFeil("Dette gjør vi også etter lunsj")
    @Test
    fun `Kan ikke overstyre arbeidsforhold for en forlengelse som allerede har tidligere utbetalinger`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode, arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1.toString(), LocalDate.EPOCH, null),
                Vilkårsgrunnlag.Arbeidsforhold(a2.toString(), 1.desember(2017), null)
            )
        )
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, utbetalingGodkjent = true)
        håndterUtbetalt(1.vedtaksperiode)
        // ny periode
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        val skjæringstidspunkt = inspektør.skjæringstidspunkt(2.vedtaksperiode)
        assertEquals(listOf(a1.toString(), a2.toString()).toList(), person.orgnummereMedRelevanteArbeidsforhold(skjæringstidspunkt).toList())
        håndterOverstyrArbeidsforhold(skjæringstidspunkt, listOf(OverstyrArbeidsforhold.ArbeidsforholdOverstyrt(a2, false)))
        assertEquals(listOf(a1.toString(), a2.toString()), person.orgnummereMedRelevanteArbeidsforhold(skjæringstidspunkt))
        assertError(2.vedtaksperiode, "Kan ikke overstyre arbeidsforhold for en pågående behandling der én eller flere perioder er behandlet ferdig", a1)
    }
}
