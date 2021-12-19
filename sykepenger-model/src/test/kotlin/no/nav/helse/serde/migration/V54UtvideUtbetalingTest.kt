package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V54UtvideUtbetalingTest {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `lagrer maksdato, forbrukteSykedager og gjenståendeSykedager på utbetalinger`() {
        val migrated = listOf(V54UtvideUtbetaling())
            .migrate(objectMapper.readTree(originalJson()))
        val expected = objectMapper.readTree(expectedJson())

        assertEquals(expected, migrated)
    }
}

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
                  "maksdato": "2020-12-24",
                  "forbrukteSykedager": 100,
                  "gjenståendeSykedager": 148
                },
                {
                  "id": "9b842ebe-699c-4a57-9265-453ca1ace142",
                  "annullert": false,
                  "maksdato": "2021-01-31",
                  "forbrukteSykedager": 80,
                  "gjenståendeSykedager": 128
                },
                {
                  "id": "be9faa2a-d11e-4d63-926c-bc217692206f",
                  "annullert": true,
                  "maksdato":"+999999999-12-31"
                }
            ],
            "vedtaksperioder": [
              {
                "utbetalingId": "c11cdfbc-dcc4-4f29-9ef9-e0db7b197487",
                "maksdato": "2020-12-24",
                "forbrukteSykedager": 100,
                "gjenståendeSykedager": 148
              }
            ],
            "forkastede": [
              {
                "vedtaksperiode": {
                    "utbetalingId": "9b842ebe-699c-4a57-9265-453ca1ace142",
                    "maksdato": "2021-01-31",
                    "forbrukteSykedager": 80,
                    "gjenståendeSykedager": 128
                }
              }
            ]
        }
    ],
    "skjemaVersjon": 54
}
"""
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
                  "annullert": false
                },
                {
                  "id": "be9faa2a-d11e-4d63-926c-bc217692206f",
                  "annullert": true
                }
            ],
            "vedtaksperioder": [
              {
                "utbetalingId": "c11cdfbc-dcc4-4f29-9ef9-e0db7b197487",
                "maksdato": "2020-12-24",
                "forbrukteSykedager": 100,
                "gjenståendeSykedager": 148
              }
            ],
            "forkastede": [
              {
                "vedtaksperiode": {
                    "utbetalingId": "9b842ebe-699c-4a57-9265-453ca1ace142",
                    "maksdato": "2021-01-31",
                    "forbrukteSykedager": 80,
                    "gjenståendeSykedager": 128
                }
              }
            ]
        }
    ],
    "skjemaVersjon": 53
}
"""

