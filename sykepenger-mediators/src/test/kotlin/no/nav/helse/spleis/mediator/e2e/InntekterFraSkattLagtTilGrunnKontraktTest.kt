package no.nav.helse.spleis.mediator.e2e

import no.nav.helse.Toggle
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.januar
import no.nav.helse.spleis.mediator.TestMessageFactory
import no.nav.helse.spleis.mediator.e2e.KontraktAssertions.assertUtgåendeMelding
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class InntekterFraSkattLagtTilGrunnKontraktTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `utkast til vedtak`() = Toggle.InntektsmeldingSomIkkeKommer.enable {
        @Language("JSON")
        val forventet = """
        {
            "@event_name": "skatteinntekter_lagt_til_grunn",
            "vedtaksperiodeId": "<uuid>",
            "behandlingId": "<uuid>",
            "aktørId": "$AKTØRID",
            "organisasjonsnummer": "$ORGNUMMER",
            "fødselsnummer": "$UNG_PERSON_FNR_2018",
            "omregnetÅrsinntekt": 372000.0,
            "skatteinntekter": [{"måned": "2017-10", "beløp": 31000.0}, {"måned": "2017-11", "beløp": 31000.0}, {"måned": "2017-12", "beløp": 31000.0}]
        }
        """

        sendSøknad(perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)))
        sendSykepengegrunnlagForArbeidsgiver(
            vedtaksperiodeIndeks = 0,
            skjæringstidspunkt = 1.januar,
            orgnummer = ORGNUMMER,
            inntekterForSykepengegrunnlag = sykepengegrunnlag(
                1.januar, listOf(
                    TestMessageFactory.InntekterForSykepengegrunnlagFraLøsning.Inntekt(INNTEKT, ORGNUMMER),
                )
            )
        )
        testRapid.assertUtgåendeMelding(forventet)
    }
}


