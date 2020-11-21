package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V48OppdragTidsstempelTest {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `setter tidsstempel fra utbetalinger`() {
        val migrated = listOf(V48OppdragTidsstempel())
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
                    "tidsstempel": "2020-05-25T00:00:00.000",
                    "personOppdrag": {},
                    "arbeidsgiverOppdrag": {}
                }
            ]
        }
    ],
    "skjemaVersjon": 47
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
                    "tidsstempel": "2020-05-25T00:00:00.000",
                    "personOppdrag": {
                      "tidsstempel": "2020-05-25T00:00:00.000"
                    },
                    "arbeidsgiverOppdrag": {
                      "tidsstempel": "2020-05-25T00:00:00.000"
                    }
                }
            ]
        }
    ],
    "skjemaVersjon": 48
}
"""
