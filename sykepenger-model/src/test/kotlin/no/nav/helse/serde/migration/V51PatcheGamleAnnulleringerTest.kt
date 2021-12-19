package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V51PatcheGamleAnnulleringerTest {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `setter annullert true`() {
        val migrated = listOf(V51PatcheGamleAnnulleringer())
            .migrate(objectMapper.readTree(originalJson()))
        val expected = objectMapper.readTree(expectedJson())

        assertEquals(expected, migrated)
    }
}

@Language("JSON")
private fun originalJson() =
    """
{
    "fødselsnummer": "fnr",
    "arbeidsgivere": [
        {
            "utbetalinger": [
                {
                    "arbeidsgiverOppdrag": {
                        "fagsystemId": "fagsystemId-2"
                    },
                    "status": "UTBETALT",
                    "annullert": false
                },
                {
                    "arbeidsgiverOppdrag": {
                        "fagsystemId": "fagsystemId-2"
                    },
                    "status": "UTBETALT",
                    "annullert": true
                },
                {
                    "arbeidsgiverOppdrag": {
                        "fagsystemId": "fagsystemId-1"
                    },
                    "annullert": false,
                    "status": "UTBETALT"
                },
                {
                    "arbeidsgiverOppdrag": {
                        "fagsystemId": "fagsystemId-2"
                    },
                    "status": "UTBETALT",
                    "annullert": false
                }
            ]
        }
    ],
    "skjemaVersjon": 50
}
"""

@Language("JSON")
private fun expectedJson() =
    """
{
    "fødselsnummer": "fnr",
    "arbeidsgivere": [
        {
            "utbetalinger": [
                {
                    "arbeidsgiverOppdrag": {
                        "fagsystemId": "fagsystemId-2"
                    },
                    "status": "UTBETALT",
                    "annullert": false
                },
                {
                    "arbeidsgiverOppdrag": {
                        "fagsystemId": "fagsystemId-2"
                    },
                    "status": "UTBETALT",
                    "annullert": true
                },
                {
                    "arbeidsgiverOppdrag": {
                        "fagsystemId": "fagsystemId-1"
                    },
                    "annullert": false,
                    "status": "UTBETALT"
                },
                {
                    "arbeidsgiverOppdrag": {
                        "fagsystemId": "fagsystemId-2"
                    },
                    "status": "UTBETALT",
                    "annullert": true
                }
            ]
        }
    ],
    "skjemaVersjon": 51
}
"""
