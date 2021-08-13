package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V113MigrerVekkFraFjernetTilstandTilAnnulleringTest {
    @Test
    fun `Migrerer til annullering til avsluttet`() {
        assertEquals(toNode(expected), migrer(original))
    }

    private fun toNode(json: String) = serdeObjectMapper.readTree(json)

    private fun migrer(json: String) = listOf(V113MigrerVekkFraFjernetTilstandTilAnnullering()).migrate(toNode(json))

    @Language("JSON")
    private val original = """{
    "arbeidsgivere": [
        {
            "vedtaksperioder": [
                {
                    "id": "39cee3ad-3fb3-4a53-a980-0f1cdfb5e5ec",
                    "tilstand": "TIL_UTBETALING"
                },
                {
                    "id": "ccecefca-20a6-40d2-a2e5-7c4971fa8243",
                    "tilstand": "TIL_ANNULLERING"
                }
            ]
        }
    ],
    "skjemaVersjon": 112
}
"""

    @Language("JSON")
    private val expected = """{
    "arbeidsgivere": [
        {
            "vedtaksperioder": [
                {
                    "id": "39cee3ad-3fb3-4a53-a980-0f1cdfb5e5ec",
                    "tilstand": "TIL_UTBETALING"
                },
                {
                    "id": "ccecefca-20a6-40d2-a2e5-7c4971fa8243",
                    "tilstand": "AVSLUTTET"
                }
            ]
        }
    ],
    "skjemaVersjon": 113
}"""


}
