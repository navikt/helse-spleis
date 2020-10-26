package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class V40RenamerFørsteFraværsdagTest {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `renamer første fraværsdag til beregningsdato`() {
        val migrated = listOf(V40RenamerFørsteFraværsdag())
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
          "førsteFraværsdag": "2020-09-01"
        },
        {
          "førsteFraværsdag": null
        },
        {
        }
      ],
      "forkastede": [
        {
          "vedtaksperiode": {
            "førsteFraværsdag": "2020-09-01"
          }
        },
        {
          "vedtaksperiode": {
            "førsteFraværsdag": null
          }
        }
      ]
    }
  ],
  "skjemaVersjon": 39
}
"""

@Language("JSON")
private fun expectedJson() =
    """{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "førsteFraværsdag": "2020-09-01",
          "beregningsdato": "2020-09-01"
        },
        {
          "førsteFraværsdag": null,
          "beregningsdato": null
        },
        {
          "beregningsdato": null
        }
      ],
      "forkastede": [
        {
          "vedtaksperiode": {
            "førsteFraværsdag": "2020-09-01",
            "beregningsdato": "2020-09-01"
          }
        },
        {
          "vedtaksperiode": {
            "førsteFraværsdag": null,
            "beregningsdato": null
          }
        }
      ]
    }
  ],
  "skjemaVersjon": 40
}
"""
