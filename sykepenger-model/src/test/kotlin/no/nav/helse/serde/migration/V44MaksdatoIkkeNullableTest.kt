package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V44MaksdatoIkkeNullableTest {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `setter default på maksdato`() {
        val migrated = listOf(V44MaksdatoIkkeNullable())
            .migrate(objectMapper.readTree(originalJson()))
        val expected = objectMapper.readTree(expectedJson())

        assertEquals(expected, migrated)
    }
}

@Language("JSON")
private fun originalJson() =
    """{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "maksdato": "2020-09-01"
        },
        {
          "maksdato": null
        },
        {
        }
      ],
      "forkastede": [
        {
          "vedtaksperiode": {
            "maksdato": "2020-09-01"
          }
        },
        {
          "vedtaksperiode": {
            "maksdato": null
          }
        }
      ]
    }
  ],
  "skjemaVersjon": 43
}
"""

@Language("JSON")
private fun expectedJson() =
    """{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "maksdato": "2020-09-01"
        },
        {
          "maksdato": "+999999999-12-31"
        },
        {
          "maksdato": "+999999999-12-31"
        }
      ],
      "forkastede": [
        {
          "vedtaksperiode": {
            "maksdato": "2020-09-01"
          }
        },
        {
          "vedtaksperiode": {
            "maksdato": "+999999999-12-31"
          }
        }
      ]
    }
  ],
  "skjemaVersjon": 44
}
"""
