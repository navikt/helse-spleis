package no.nav.helse.spleis.mediator.e2e

import java.util.UUID
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.inntektsmeldingkontrakt.Periode
import org.junit.jupiter.api.Test

internal class InntektsopplysningerFraLagretInntektsmeldingTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `kan bruke inntektsopplysninger fra lagret navno-inntektsmelding`() {
        val (meldingsreferanseIdInntektsmelding, _) = sendNavNoInntektsmelding(listOf(1.januar til 16.januar), UUID.randomUUID())
        sendSøknad(perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)))
        assertTilstand(0, "AVVENTER_INNTEKTSMELDING")

        sendInntektsopplysningerFraLagretInntektsmelding(testRapid.inspektør.vedtaksperiodeId(0), meldingsreferanseIdInntektsmelding)
        assertTilstand(0, "AVVENTER_VILKÅRSPRØVING")
    }

    @Test
    fun `kan bruke inntektsopplysninger fra lagret lps-inntektsmelding`() {
        val (meldingsreferanseIdInntektsmelding, _) = sendInntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 1.januar)
        sendSøknad(perioder = listOf(SoknadsperiodeDTO(fom = 1.mars, tom = 31.mars, sykmeldingsgrad = 100)))
        assertTilstand(0, "AVVENTER_INNTEKTSMELDING")

        sendInntektsopplysningerFraLagretInntektsmelding(testRapid.inspektør.vedtaksperiodeId(0), meldingsreferanseIdInntektsmelding)
        assertTilstand(0, "AVVENTER_VILKÅRSPRØVING")
    }
}
