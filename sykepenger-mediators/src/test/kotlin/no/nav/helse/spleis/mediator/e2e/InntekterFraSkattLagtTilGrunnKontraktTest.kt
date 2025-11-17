package no.nav.helse.spleis.mediator.e2e

import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.januar
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.spleis.mediator.e2e.KontraktAssertions.assertUtgåendeMelding
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class InntekterFraSkattLagtTilGrunnKontraktTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `skatteinntekter lagt til grunn`() {
        @Language("JSON")
        val forventet = """
        {
            "@event_name": "skatteinntekter_lagt_til_grunn",
            "vedtaksperiodeId": "<uuid>",
            "behandlingId": "<uuid>",
            "skjæringstidspunkt": "2018-01-01",
            "organisasjonsnummer": "$ORGNUMMER",
            "yrkesaktivitetstype": "ARBEIDSTAKER",
            "fødselsnummer": "$UNG_PERSON_FNR_2018",
            "omregnetÅrsinntekt": 372000.0,
            "skatteinntekter": [{"måned": "2017-10", "beløp": 31000.0}, {"måned": "2017-11", "beløp": 31000.0}, {"måned": "2017-12", "beløp": 31000.0}]
        }
        """

        sendSøknad(perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)))
        sendNyPåminnelse(0, TilstandType.AVVENTER_INNTEKTSMELDING, flagg = setOf("ønskerInntektFraAOrdningen"))
        sendVilkårsgrunnlag(0)
        assertTilstander(0, "AVVENTER_INFOTRYGDHISTORIKK", "AVVENTER_INNTEKTSMELDING", "AVVENTER_BLOKKERENDE_PERIODE", "AVVENTER_VILKÅRSPRØVING", "AVVENTER_HISTORIKK")
        testRapid.assertUtgåendeMelding(forventet)
    }
}


