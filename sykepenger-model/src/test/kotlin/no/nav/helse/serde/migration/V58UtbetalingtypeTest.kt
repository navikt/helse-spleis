package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V58UtbetalingtypeTest {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `lager avstemmingsnøkkel på utbetalinger`() {
        val migrated = listOf(V58Utbetalingtype())
            .migrate(objectMapper.readTree(originalJson()))
        val expected = objectMapper.readTree(expectedJson())

        assertEquals(expected, migrated)
    }
}

@Language("JSON")
private fun originalJson() =
    """
{
    "arbeidsgivere": [
        {
            "utbetalinger": [
                {
                  "id": "c11cdfbc-dcc4-4f29-9ef9-e0db7b197487",
                  "annullert": false
                },
                {
                  "id": "9b842ebe-699c-4a57-9265-453ca1ace142",
                  "annullert": true
                }
            ]
        }
    ],
    "skjemaVersjon": 57
}
"""

@Language("JSON")
private fun expectedJson() =
    """
{
    "arbeidsgivere": [
        {
            "utbetalinger": [
                {
                  "id": "c11cdfbc-dcc4-4f29-9ef9-e0db7b197487",
                  "annullert": false,
                  "type": "UTBETALING"
                },
                {
                  "id": "9b842ebe-699c-4a57-9265-453ca1ace142",
                  "annullert": true,
                  "type": "ANNULLERING"
                }
            ]
        }
    ],
    "skjemaVersjon": 58
}
"""
