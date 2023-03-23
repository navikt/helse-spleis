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
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class GodkjenningsbehovTagsTest : AbstractEndToEndTest() {

    @Test
    fun `arbeidsgiverutbetaling`() {
        tilGodkjenning(1.januar, 31.januar, a1)
        assertEquals(setOf("ARBEIDSGIVERUTBETALING"), tags())
    }

    @Test
    fun `personutbetaling`() {
        nyPeriode(1.januar til 31.januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar), refusjon = Inntektsmelding.Refusjon(beløp = Inntekt.INGEN, opphørsdato = null))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        assertEquals(setOf("PERSONUTBETALING"), tags())
    }

    @Test
    fun `delvis refusjon`() {
        nyPeriode(1.januar til 31.januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar), refusjon = Inntektsmelding.Refusjon(beløp = INNTEKT/2, opphørsdato = null))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        assertEquals(setOf("ARBEIDSGIVERUTBETALING", "PERSONUTBETALING"), tags())
    }

    @Test
    fun `ingen utbetaling`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent), Søknad.Søknadsperiode.Ferie(1.februar, 28.februar))
        håndterYtelser(2.vedtaksperiode)
        assertEquals(setOf("INGEN_UTBETALING"), tags(vedtaksperiodeId = 2.vedtaksperiode.id(a1)))
    }

    private fun tags(vedtaksperiodeId: UUID = 1.vedtaksperiode.id(a1)) = hendelselogg.etterspurtBehov<Set<String>>(
        vedtaksperiodeId = vedtaksperiodeId,
        behov = Aktivitet.Behov.Behovtype.Godkjenning,
        felt = "tags"
    ) ?: emptySet()
}
