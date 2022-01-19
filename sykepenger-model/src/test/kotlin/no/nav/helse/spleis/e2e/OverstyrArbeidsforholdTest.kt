package no.nav.helse.spleis.e2e

import no.nav.helse.ForventetFeil
import no.nav.helse.hendelser.*
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class OverstyrArbeidsforholdTest : AbstractEndToEndTest() {
    @ForventetFeil("skal fikses i morgen 🙂")
    @Test
    fun `fjerner et ghost tilfelle ved hjelp av overstyring`() {
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
}
