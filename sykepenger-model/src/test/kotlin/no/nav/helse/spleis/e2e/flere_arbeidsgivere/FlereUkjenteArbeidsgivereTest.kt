package no.nav.helse.spleis.e2e.flere_arbeidsgivere

import no.nav.helse.assertForventetFeil
import no.nav.helse.februar
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SV_2
import no.nav.helse.person.inntekt.Inntektsmelding
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertForkastetPeriodeTilstander
import no.nav.helse.spleis.e2e.assertFunksjonellFeil
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class FlereUkjenteArbeidsgivereTest : AbstractEndToEndTest() {

    @Test
    fun `én arbeidsgiver blir to - søknad for ag1 først`() {
        nyttVedtak(1.januar, 31.januar, orgnummer = a1)
        nyPeriode(1.februar til 20.februar, a1)
        nyPeriode(1.februar til 20.februar, a2)
        håndterInntektsmelding(listOf(1.februar til 16.februar), orgnummer = a2)
        assertFunksjonellFeil("Minst en arbeidsgiver inngår ikke i sykepengegrunnlaget", 2.vedtaksperiode.filter(a1))
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTrue(inspektør(a1).periodeErForkastet(2.vedtaksperiode))
        assertTrue(inspektør(a2).periodeErForkastet(1.vedtaksperiode))
    }

    @Test
    fun `én arbeidsgiver blir to - forlenges kun av ny ag`() {
        nyttVedtak(1.januar, 31.januar, orgnummer = a1)
        nyPeriode(1.februar til 20.februar, a2)
        håndterInntektsmelding(listOf(1.februar til 16.februar), orgnummer = a2)
        val vilkårsgrunnlag = inspektør.vilkårsgrunnlag(1.vedtaksperiode)
        assertNotNull(vilkårsgrunnlag)
        val sykepengegrunnlagInspektør = vilkårsgrunnlag.inspektør.sykepengegrunnlag.inspektør
        assertEquals(1, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)

        sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
            assertEquals(INNTEKT, it.inntektsopplysning.inspektør.beløp)
            assertEquals(Inntektsmelding::class, it.inntektsopplysning::class)
        }
        assertNull(sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver[a2])


        assertFunksjonellFeil(RV_SV_2, 1.vedtaksperiode.filter(a2))
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTrue(inspektør(a2).periodeErForkastet(1.vedtaksperiode))
    }

    @Test
    fun `én arbeidsgiver blir to - førstegangsbehandlingen hos ag2 forkastes`() {
        nyttVedtak(1.januar, 31.januar, orgnummer = a1)
        forlengVedtak(1.februar, 28.februar, orgnummer = a1)
        forlengVedtak(1.mars, 31.mars, orgnummer = a1)

        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT + 500.daglig, orgnummer = a1)
        håndterYtelser(3.vedtaksperiode, orgnummer = a1)
        håndterSimulering(3.vedtaksperiode, orgnummer = a1)

        håndterInntektsmelding(listOf(1.februar til 16.februar), begrunnelseForReduksjonEllerIkkeUtbetalt = "TidligereVirksomhet", orgnummer = a2)
        nullstillTilstandsendringer()
        nyPeriode(1.februar til 20.februar, a2)

        assertForventetFeil(
            forklaring = "1.vedtaksperiode er i feil tilstand. Revurderingen blir sittende fast",
            nå = {
                assertTilstander(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a1)
                assertTilstander(2.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_REVURDERING, orgnummer = a1)
                assertTilstander(3.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING, AVVENTER_REVURDERING, orgnummer = a1)
                assertForkastetPeriodeTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, TIL_INFOTRYGD, orgnummer = a2)
            },
            ønsket = {
                assertTilstander(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a1)
                assertTilstander(2.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a1)
                assertTilstander(3.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a1)
                assertForkastetPeriodeTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, TIL_INFOTRYGD, orgnummer = a2)
            }
        )

    }
}