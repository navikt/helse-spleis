package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V71SetteOpprettetTidspunktTest {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `legger på opprettet-tidspunkt`() {
        val migrated = listOf(V71SetteOpprettetTidspunkt())
            .migrate(objectMapper.readTree(originalJson()))
        val expected = objectMapper.readTree(expectedJson())

        assertEquals(expected, migrated)
    }
}

@Language("JSON")
private fun originalJson() =
    """
{
  "aktivitetslogg": {
    "aktiviteter": [
      {
        "tidsstempel": "2020-11-20 12:00:00.100"
      },
      {
        "tidsstempel": "2020-11-30 09:00:00.200"
      },
      {
        "tidsstempel": "2020-12-12 15:10:10.100"
      }
    ]},
    "skjemaVersjon": 70
}
"""

@Language("JSON")
private fun expectedJson() =
    """
{
  "aktivitetslogg": {
    "aktiviteter": [
      {
        "tidsstempel": "2020-11-20 12:00:00.100"
      },
      {
        "tidsstempel": "2020-11-30 09:00:00.200"
      },
      {
        "tidsstempel": "2020-12-12 15:10:10.100"
      }
    ]},
    "opprettet": "2020-11-20T12:00:00.100",
    "skjemaVersjon": 71
}
"""
