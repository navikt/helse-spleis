package no.nav.helse.spleis.e2e

import no.nav.helse.Toggle
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class ArbeidsgiveropplysningerTest: AbstractEndToEndTest() {

    @Test
    fun `sender ut event TrengerArbeidsgiveropplysninger når vi ankommer AvventerInntektsmeldingEllerHistorikk`() = Toggle.Splarbeidsbros.enable {
        nyPeriode(1.januar til 31.januar)
        Assertions.assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `sender ikke ut event TrengerArbeidsgiveropplysninger med toggle disabled`() = Toggle.Splarbeidsbros.disable {
        nyPeriode(1.januar til 31.januar)
        Assertions.assertEquals(0, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }
}