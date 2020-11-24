package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V59UtbetalingNesteForrigeTest {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `lager avstemmingsnøkkel på utbetalinger`() {
        val migrated = listOf(V59UtbetalingNesteForrige())
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
                  "arbeidsgiverOppdrag": {
                    "fagsystemId": "fagsystem1"
                  }
                },
                {
                  "id": "9b842ebe-699c-4a57-9265-453ca1ace142",
                  "arbeidsgiverOppdrag": {
                    "fagsystemId": "fagsystem2"
                  }
                },
                {
                  "id": "236f03d8-2c82-4f74-a904-097fb36de7a4",
                  "arbeidsgiverOppdrag": {
                    "fagsystemId": "fagsystem2"
                  }
                },
                {
                  "id": "1e6d9fa9-ee15-4a76-a158-a1ca81b65d36",
                  "arbeidsgiverOppdrag": {
                    "fagsystemId": "fagsystem2"
                  }
                },
                {
                  "id": "069db47d-4c99-44c7-bfa3-04e9d5302e6f",
                  "arbeidsgiverOppdrag": {
                    "fagsystemId": "fagsystem3"
                  }
                },
                {
                  "id": "56ed7093-e468-485a-9702-abe3b69c5d90",
                  "arbeidsgiverOppdrag": {
                    "fagsystemId": "fagsystem1"
                  }
                }
            ]
        }
    ],
    "skjemaVersjon": 58
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
                  "neste": "56ed7093-e468-485a-9702-abe3b69c5d90",
                  "arbeidsgiverOppdrag": {
                    "fagsystemId": "fagsystem1"
                  }
                },
                {
                  "id": "9b842ebe-699c-4a57-9265-453ca1ace142",
                  "neste": "236f03d8-2c82-4f74-a904-097fb36de7a4",
                  "arbeidsgiverOppdrag": {
                    "fagsystemId": "fagsystem2"
                  }
                },
                {
                  "id": "236f03d8-2c82-4f74-a904-097fb36de7a4",
                  "forrige": "9b842ebe-699c-4a57-9265-453ca1ace142",
                  "neste": "1e6d9fa9-ee15-4a76-a158-a1ca81b65d36",
                  "arbeidsgiverOppdrag": {
                    "fagsystemId": "fagsystem2"
                  }
                },
                {
                  "id": "1e6d9fa9-ee15-4a76-a158-a1ca81b65d36",
                  "forrige": "236f03d8-2c82-4f74-a904-097fb36de7a4",
                  "arbeidsgiverOppdrag": {
                    "fagsystemId": "fagsystem2"
                  }
                },
                {
                  "id": "069db47d-4c99-44c7-bfa3-04e9d5302e6f",
                  "arbeidsgiverOppdrag": {
                    "fagsystemId": "fagsystem3"
                  }
                },
                {
                  "id": "56ed7093-e468-485a-9702-abe3b69c5d90",
                  "forrige": "c11cdfbc-dcc4-4f29-9ef9-e0db7b197487",
                  "arbeidsgiverOppdrag": {
                    "fagsystemId": "fagsystem1"
                  }
                }
            ]
        }
    ],
    "skjemaVersjon": 59
}
"""
