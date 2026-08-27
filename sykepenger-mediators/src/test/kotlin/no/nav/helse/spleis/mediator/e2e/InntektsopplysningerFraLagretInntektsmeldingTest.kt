package no.nav.helse.spleis.mediator.e2e

import java.util.UUID
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import org.junit.jupiter.api.Test

internal class InntektsopplysningerFraLagretInntektsmeldingTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `kan bruke inntektsopplysninger fra lagret navno-inntektsmelding`() {
        val (meldingsreferanseIdInntektsmelding, _) = sendNavNoInntektsmelding(listOf(1.januar til 16.januar), vedtaksperiodeUtfisker = VedtaksperiodeUtfisker.Eksplisitt(UUID.randomUUID()))
        assertMeldingOmMeldingIkkeHåndtertFordiPersonIkkeFunnet("arbeidsgiveropplysninger", meldingsreferanseIdInntektsmelding.toString())
        sendSøknad(perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)))
        assertTilstand(0, "AVVENTER_INNTEKTSMELDING")

        sendInntektsopplysningerFraLagretInntektsmelding(testRapid.inspektør.vedtaksperiodeId(0), meldingsreferanseIdInntektsmelding)
        assertTilstand(0, "AVVENTER_VILKÅRSPRØVING")
    }
}
