package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V111RiktigStatusAnnullerteUtbetalingerTest {
    @Test
    fun `Sletter arbeidsforhold`() {
        assertEquals(toNode(expected), migrer(original))
    }

    private fun toNode(json: String) = serdeObjectMapper.readTree(json)

    private fun migrer(json: String) = listOf(V111RiktigStatusAnnullerteUtbetalinger()).migrate(toNode(json))

    @Language("JSON")
    private val original = """{
    "arbeidsgivere": [
        {
            "utbetalinger": [
                {
                    "status": "IKKE_UTBETALT",
                    "type": "ANNULLERING"
                },
                {
                    "status": "UTBETALT",
                    "type": "ANNULLERING"
                },
                {
                    "status": "ANNULLERT",
                    "type": "UTBETALING"
                },
                 {
                    "type": "UTBETALING"
                },
                 {
                    "status": "ANNULLERT"
                }
            ]
        }
    ],
    "skjemaVersjon": 110
}
"""


    @Language("JSON")
    private val expected = """{
    "arbeidsgivere": [
        {
            "utbetalinger": [
                {
                    "status": "IKKE_UTBETALT",
                    "type": "ANNULLERING"
                },
                {
                    "status": "ANNULLERT",
                    "type": "ANNULLERING"
                },
                {
                    "status": "ANNULLERT",
                    "type": "UTBETALING"
                },
                 {
                    "type": "UTBETALING"
                },
                 {
                    "status": "ANNULLERT"
                }
            ]
        }
    ],
    "skjemaVersjon": 111
}
"""
}
