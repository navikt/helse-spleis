package no.nav.helse.spleis.e2e

import no.nav.helse.Toggle
import no.nav.helse.hendelser.Arbeidsgiveropplysninger
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import org.junit.jupiter.api.Test

internal class ArbeidsgiveropplysningTest: AbstractEndToEndTest() {

    @Test
    fun `sender ut behov om arbeidsgiveropplysninger når vi ankommer AvventerInntektsmeldingEllerHistorikk`() = Toggle.Splarbeidsbros.enable {
        nyPeriode(1.januar til 31.januar)
        assertEtterspurt(
            løsning = Arbeidsgiveropplysninger::class,
            type = Behovtype.Arbeidsgiveropplysninger,
            1.vedtaksperiode,
            ORGNUMMER
        )
    }

    @Test
    fun `sender ikke ut behov om arbeidsgiveropplysninger når vi ankommer AvventerInntektsmeldingEllerHistorikk`() = Toggle.Splarbeidsbros.disable {
        nyPeriode(1.januar til 31.januar)
        assertIkkeEtterspurt(
            løsning = Arbeidsgiveropplysninger::class,
            type = Behovtype.Arbeidsgiveropplysninger,
            1.vedtaksperiode,
            ORGNUMMER
        )
    }
}