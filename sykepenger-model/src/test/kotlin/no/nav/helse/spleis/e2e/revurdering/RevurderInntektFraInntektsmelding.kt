package no.nav.helse.spleis.e2e.revurdering

import no.nav.helse.hendelser.Inntektsmelding.Refusjon
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.assertRefusjonsbeløp
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.tilGodkjenning
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class RevurderInntektFraInntektsmelding: AbstractEndToEndTest() {

    @Test
    fun `Korrigerende inntektsmelding før vedtak fattet - endrer inntekt og refusjon`() {
        tilGodkjenning(fom = 1.januar, tom = 31.januar, beregnetInntekt = INNTEKT, grad = 100.prosent, førsteFraværsdag = 1.januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT * 1.5, refusjon = Refusjon(INNTEKT * 1.5, null))
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        håndterYtelser(1.vedtaksperiode)

        inspektør.refusjonsopplysningerFraVilkårsgrunnlag().assertRefusjonsbeløp(1.januar til 31.januar, INNTEKT * 1.5)
        val arbeidsgiveropplysningerInspektør = inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.inspektør?.sykepengegrunnlag?.inspektør?.arbeidsgiverInntektsopplysninger?.first()?.inspektør!!
        assertEquals(INNTEKT * 1.5, arbeidsgiveropplysningerInspektør.inntektsopplysning.omregnetÅrsinntekt())

    }

    @Test
    fun `Korrigerende inntektsmelding før vedtak fattet endrer arbeidsgiverperiode`() {
        tilGodkjenning(fom = 1.januar, tom = 31.januar, grad = 100.prosent, førsteFraværsdag = 1.januar)
        håndterInntektsmelding(listOf(2.januar til 17.januar))
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        håndterYtelser(1.vedtaksperiode)

        TODO("må se på varseltekst")
    }
}