package no.nav.helse.spleis.e2e.arbeidsgiveropplysninger

import java.util.UUID
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.hendelser.Arbeidsgiveropplysning
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_28
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_4
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.spleis.e2e.InntektsmeldingId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class HarFlereArbeidsforholdTest: AbstractDslTest() {

    @Test
    fun `Får ikke varsel om flere arbeidsforhold når det ikke er flere arbeidsforhold`() {
        a1 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(vedtaksperiodeId = 1.vedtaksperiode, arbeidsgiverperioder = listOf(1.januar til 16.januar), harFlereArbeidsforhold = false)
        }
    }

    @Test
    fun `Kaster ut søknad og ber om at det lages oppgave når det er flere arbeidsforhold`() {
        a1 {
            val søknadId = UUID.randomUUID()
            håndterSøknad(januar, søknadId = søknadId)
            val imId = håndterArbeidsgiveropplysninger(vedtaksperiodeId = 1.vedtaksperiode, arbeidsgiverperioder = listOf(1.januar til 16.januar), harFlereArbeidsforhold = true)
            assertSisteForkastetTilstand(1.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
            assertFunksjonellFeil(RV_IM_28, 1.vedtaksperiode.filter())
            val vpForkastetEvent = observatør.forkastet(1.vedtaksperiode)
            assertEquals(setOf(søknadId, imId), vpForkastetEvent.hendelser)
            assertEquals(1.vedtaksperiode, vpForkastetEvent.vedtaksperiodeId)
            assertEquals(emptyList<InntektsmeldingId>(), observatør.inntektsmeldingIkkeHåndtert)

            assertEquals(listOf(imId to 1.vedtaksperiode), observatør.inntektsmeldingHåndtert)
        }
    }

    @Test
    fun `Får varsel om flere arbeidsforhold når det er flere arbeidsforhold på korrigerte arbeidsgiveropplysninger`() {
        a1 {
            nyttVedtak(januar)
            håndterKorrigerteArbeidsgiveropplysninger(vedtaksperiodeId = 1.vedtaksperiode, Arbeidsgiveropplysning.HarFlereArbeidsforhold, Arbeidsgiveropplysning.OppgittInntekt(INNTEKT))
            assertSisteTilstand(1.vedtaksperiode, TilstandType.AVVENTER_HISTORIKK_REVURDERING)
            assertVarsler(1.vedtaksperiode, RV_IM_28, RV_IM_4)
        }
    }
}
