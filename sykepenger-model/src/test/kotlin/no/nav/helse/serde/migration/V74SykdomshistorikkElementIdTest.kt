package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

internal class V74SykdomshistorikkElementIdTest {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `lager id på sykdomshistorikk`() {
        val migrated = listOf(V74SykdomshistorikkElementId())
            .migrate(objectMapper.readTree(originalJson()))
        val element = migrated
            .path("arbeidsgivere")
            .path(0)
            .path("sykdomshistorikk")
            .path(0)

        assertTrue(element.hasNonNull("id"))
        assertDoesNotThrow { UUID.fromString(element.path("id").asText()) }
        assertEquals(74, migrated.path("skjemaVersjon").intValue())
    }
}

@Language("JSON")
private fun originalJson() =
    """
{
    "arbeidsgivere": [
        {
            "sykdomshistorikk": [
                {
                }
            ]
        }
    ],
    "skjemaVersjon": 73
}
"""
