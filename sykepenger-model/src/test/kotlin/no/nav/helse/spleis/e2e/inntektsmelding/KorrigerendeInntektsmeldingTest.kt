package no.nav.helse.spleis.e2e.inntektsmelding

import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.Varselkode.RV_IM_4
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertIngenVarsel
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.nyttVedtak
import org.junit.jupiter.api.Test

internal class KorrigerendeInntektsmeldingTest: AbstractEndToEndTest() {

    @Test
    fun `Avsluttet vedtaksperiode skal ikke få varsel ved korrigerende inntektsmelding`() {
        nyttVedtak(1.januar, 31.januar)

        håndterInntektsmelding(listOf(1.januar til 16.januar))

        assertIngenVarsel(RV_IM_4)
    }

}