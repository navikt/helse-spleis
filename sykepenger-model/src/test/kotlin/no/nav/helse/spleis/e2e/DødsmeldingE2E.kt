package no.nav.helse.spleis.e2e

import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.februar
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class DødsmeldingE2E : AbstractDslTest() {

    @Test
    fun `registrerer dødsdato`() {
        val dødsdato = 10.januar
        håndterDødsmelding(dødsdato)
        assertEquals(dødsdato, inspiser(personInspektør).dødsdato)
    }

    @Test
    fun `Dager etter dødsdato avvises`() {
        håndterDødsmelding(18.januar)
        håndterSøknad(januar)
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), INNTEKT)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        assertEquals(9, inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.utbetalingstidslinje.inspektør.avvistDagTeller)
    }

    @Test
    fun `Ingen dager avvises når dødsdato er etter perioden`() {
        håndterDødsmelding(1.februar)
        håndterSøknad(januar)
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), INNTEKT)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        assertEquals(0, inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.utbetalingstidslinje.inspektør.avvistDagTeller)
    }

    @Test
    fun `Alle dager avvises når dødsdato er før perioden`() {
        håndterDødsmelding(31.desember(2017))
        håndterSøknad(januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.utbetalingstidslinje.inspektør.also {
            assertEquals(11, it.avvistDagTeller)
            assertEquals(16, it.arbeidsgiverperiodeDagTeller)
            assertEquals(4, it.navHelgDagTeller)
        }
    }
}
