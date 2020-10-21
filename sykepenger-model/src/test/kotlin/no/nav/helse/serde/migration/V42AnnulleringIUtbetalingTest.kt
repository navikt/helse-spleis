package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class V42AnnulleringIUtbetalingTest {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `renamer beregningsdato til beregningsdatoFraInfotrygd`() {
        val migrated = listOf(V42AnnulleringIUtbetaling())
            .migrate(objectMapper.readTree(originalJson()))
        val expected = objectMapper.readTree(expectedJson())

        assertEquals(expected, migrated)
    }
}

@Language("JSON")
private fun originalJson() = """{
    "arbeidsgivere": [
        {
            "utbetalinger": [
                {
                    "status": "IKKE_UTBETALT"
                },
                {
                    "status": "UTBETALT"
                },
                {
                    "status": "ANNULLERT"
                }
            ]
        }
    ],
    "skjemaVersjon": 41
}
"""

@Language("JSON")
private fun expectedJson() = """{
    "arbeidsgivere": [
        {
            "utbetalinger": [
                {
                    "status": "IKKE_UTBETALT",
                    "annullert": false
                },
                {
                    "status": "UTBETALT",
                    "annullert": false
                },
                {
                    "status": "UTBETALT",
                    "annullert": true
                }
            ]
        }
    ],
    "skjemaVersjon": 42
}
"""
