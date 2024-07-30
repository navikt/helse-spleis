package no.nav.helse.spleis.mediator.e2e

import no.nav.helse.spleis.mediator.e2e.KontraktAssertions.assertUtgåendeMelding
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class UtkastTilVedtakKontraktTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `utkast til vedtak`() {
        nyttVedtak()
        @Language("JSON")
        val forventet = """
        {
            "@event_name": "utkast_til_vedtak",
            "vedtaksperiodeId": "<uuid>",
            "behandlingId": "<uuid>",
            "skjæringstidspunkt": "2018-01-01",
            "sykepengegrunnlagsfakta": {
              "6G": 561804.0
            },
            "aktørId": "$AKTØRID",
            "fødselsnummer": "$UNG_PERSON_FNR_2018",
            "tags": [
                "Førstegangsbehandling",
                "EnArbeidsgiver",
                "Arbeidsgiverutbetaling",
                "Innvilget"
            ]
        }
        """
        testRapid.assertUtgåendeMelding(forventet)
    }
}


