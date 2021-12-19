package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*

internal class V49UtbetalingIdTest {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `lager id på utbetalinger`() {
        val migrated = listOf(V49UtbetalingId())
            .migrate(objectMapper.readTree(originalJson()))
        val utbetaling = migrated
            .path("arbeidsgivere")
            .path(0)
            .path("utbetalinger")
            .path(0)

        assertTrue(utbetaling.hasNonNull("id"))
        assertDoesNotThrow { UUID.fromString(utbetaling.path("id").asText()) }
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
                }
            ]
        }
    ],
    "skjemaVersjon": 47
}
"""
