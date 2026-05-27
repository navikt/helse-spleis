package no.nav.helse.spleis.e2e.arbeidsgiveropplysninger

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.hendelser.Arbeidsgiveropplysning
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_28
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_4
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
    fun `Får varsel om flere arbeidsforhold når det er flere arbeidsforhold`() {
        a1 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(vedtaksperiodeId = 1.vedtaksperiode, arbeidsgiverperioder = listOf(1.januar til 16.januar), harFlereArbeidsforhold = true)
            assertVarsler(1.vedtaksperiode, RV_IM_28)
        }
    }

    @Test
    fun `Får varsel om flere arbeidsforhold når det er flere arbeidsforhold på korrigerte arbeidsgiveropplysninger`() {
        a1 {
            nyttVedtak(januar)
            håndterKorrigerteArbeidsgiveropplysninger(vedtaksperiodeId = 1.vedtaksperiode, Arbeidsgiveropplysning.HarFlereArbeidsforhold, Arbeidsgiveropplysning.OppgittInntekt(INNTEKT))
            assertVarsler(1.vedtaksperiode, RV_IM_28, RV_IM_4)
        }
    }
}
