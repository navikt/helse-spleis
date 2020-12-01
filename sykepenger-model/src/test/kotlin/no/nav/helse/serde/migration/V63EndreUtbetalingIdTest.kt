package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V63EndreUtbetalingIdTest {
    @Test
    fun `endrer id på en utbetaling`() {
        val expected = serdeObjectMapper.readTree(expectedJson)
        val migrated = listOf(V63EndreUtbetalingId())
            .migrate(serdeObjectMapper.readTree(originalJson))
        assertEquals(expected, migrated)
    }
}

@Language("JSON")
private val originalJson =
    """
{
    "arbeidsgivere": [
        {
            "utbetalinger": [
                {
                    "id": "399645a5-283c-4a3f-9b2a-f360b8170d98"
                },
                {
                    "id": "3264ed40-ca5b-4af0-8591-5ee74df8df89"
                }
            ]
        }
    ],
    "skjemaVersjon": 62
}
"""

@Language("JSON")
private val expectedJson =
    """
{
    "arbeidsgivere": [
        {
            "utbetalinger": [
                {
                    "id": "399645a5-283c-4a3f-9b2a-f360b8170d98"
                },
                {
                    "id": "bd61804d-2e98-427e-bc78-f494fd9747d6",
                    "status": "SENDT"
                }
            ]
        }
    ],
    "skjemaVersjon": 63
}
"""
