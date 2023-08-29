package no.nav.helse.spleis.e2e

import java.util.UUID
import no.nav.helse.etterspurtBehov
import no.nav.helse.februar
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class GodkjenningsbehovTagsTest : AbstractEndToEndTest() {

    @Test
    fun `arbeidsgiverutbetaling`() {
        tilGodkjenning(1.januar, 31.januar, a1)
        assertTags(setOf("ARBEIDSGIVERUTBETALING"))
    }

    @Test
    fun `personutbetaling`() {
        nyPeriode(1.januar til 31.januar)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(beløp = Inntekt.INGEN, opphørsdato = null)
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        assertTags(setOf("PERSONUTBETALING"))
    }

    @Test
    fun `delvis refusjon`() {
        nyPeriode(1.januar til 31.januar)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(beløp = INNTEKT/2, opphørsdato = null)
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        assertTags(setOf("ARBEIDSGIVERUTBETALING", "PERSONUTBETALING"))
    }

    @Test
    fun `ingen utbetaling`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent), Søknad.Søknadsperiode.Ferie(1.februar, 28.februar))
        håndterYtelser(2.vedtaksperiode)
        assertTags(setOf("INGEN_UTBETALING"), vedtaksperiodeId = 2.vedtaksperiode.id(a1))
    }

    @Test
    fun `trekker tilbake penger fra arbeidsgiver og flytter til bruker`() {
        nyttVedtak(1.januar, 31.januar)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(beløp = Inntekt.INGEN, opphørsdato = null)
        )
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        assertTags(setOf("NEGATIV_ARBEIDSGIVERUTBETALING", "PERSONUTBETALING"))
    }

    @Test
    fun `trekker tilbake penger fra person og flytter til arbeidsgiver`() {
        nyttVedtak(1.januar, 31.januar, refusjon = Inntektsmelding.Refusjon(INGEN, null))
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(beløp = INNTEKT, opphørsdato = null)
        )
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        assertTags(setOf("NEGATIV_PERSONUTBETALING", "ARBEIDSGIVERUTBETALING"))
    }

    @Test
    fun `6G-begrenset`() {
        tilGodkjenning(
            1.januar,
            31.januar,
            a1,
            beregnetInntekt = 50000.månedlig
        )
        assertTags(setOf("ARBEIDSGIVERUTBETALING", "6G_BEGRENSET"))
    }

    @Test
    fun `flere arbeidsgivere`() {
        tilGodkjenning(1.januar, 31.januar, a1, a2)
        assertTags(setOf("ARBEIDSGIVERUTBETALING", "FLERE_ARBEIDSGIVERE"))
    }

    private fun assertTags(tags: Set<String>, vedtaksperiodeId: UUID = 1.vedtaksperiode.id(a1)) {
        val actual = tags(vedtaksperiodeId)
        assertTrue(actual.containsAll(tags))
    }

    private fun tags(vedtaksperiodeId: UUID = 1.vedtaksperiode.id(a1)) = hendelselogg.etterspurtBehov<Set<String>>(
        vedtaksperiodeId = vedtaksperiodeId,
        behov = Aktivitet.Behov.Behovtype.Godkjenning,
        felt = "tags"
    ) ?: emptySet()
}
