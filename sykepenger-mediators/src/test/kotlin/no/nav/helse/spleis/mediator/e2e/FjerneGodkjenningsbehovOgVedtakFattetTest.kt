package no.nav.helse.spleis.mediator.e2e

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.januar
import no.nav.helse.spleis.mediator.e2e.KontraktAssertions.assertUtgåendeMelding
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class FjerneGodkjenningsbehovOgVedtakFattetTest: AbstractEndToEndMediatorTest() {

    @Test
    fun `avsluttet uten vedtak`() {
        val søknadId = sendSøknad(perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 16.januar, sykmeldingsgrad = 100)))

        @Language("JSON")
        val forventetAvsluttetUtenVedtak = """
        {
            "@event_name": "avsluttet_uten_vedtak",
            "aktørId": "$AKTØRID",
            "fødselsnummer": "$UNG_PERSON_FNR_2018",
            "organisasjonsnummer": "$ORGNUMMER",
            "fom" : "2018-01-01",
            "tom" : "2018-01-16",
            "skjæringstidspunkt": "2018-01-01",
            "hendelser": ["$søknadId"],
            "vedtaksperiodeId": "<uuid>",
            "generasjonId": "<uuid>",
            "behandlingId": "<uuid>",
            "avsluttetTidspunkt": "<timestamp>"
        }
        """

        testRapid.assertUtgåendeMelding(forventetAvsluttetUtenVedtak)

        assertEquals(emptyList<JsonNode>(), testRapid.inspektør.meldinger("avsluttet_med_vedtak"))
    }
}