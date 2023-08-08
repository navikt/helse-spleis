package no.nav.helse.spleis.e2e.inntektsmelding

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_4
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertIngenVarsel
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Test

internal class KorrigerendeInntektsmeldingTest: AbstractEndToEndTest() {

    @Test
    fun `Avsluttet vedtaksperiode skal ikke få varsel ved helt lik korrigerende inntektsmelding`() {
        nyttVedtak(1.januar, 31.januar)

        håndterInntektsmelding(listOf(1.januar til 16.januar))

        assertIngenVarsel(RV_IM_4)
    }

    @Test
    fun `Avsluttet vedtaksperiode skal ikke få varsel ved korrigerende inntektsmelding med endring i agp`() {
        nyttVedtak(1.januar, 31.januar)

        håndterInntektsmelding(listOf(2.januar til 17.januar))

        assertVarsel(RV_IM_4)
    }

    @Test
    fun `Avsluttet vedtaksperiode skal ikke få varsel ved korrigerende inntektsmelding med endring i refusjonsbeløp`() {
        nyttVedtak(1.januar, 31.januar)

        håndterInntektsmelding(listOf(1.januar til 16.januar), refusjon = Inntektsmelding.Refusjon(2000.månedlig, null, emptyList()))

        assertVarsel(RV_IM_4)
    }

}