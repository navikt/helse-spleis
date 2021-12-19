package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V41RenamerBeregningsdatoTest {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `renamer beregningsdato til beregningsdatoFraInfotrygd`() {
        val migrated = listOf(V41RenamerBeregningsdato())
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
          "beregningsdato": "2020-09-01"
        },
        {
          "beregningsdato": null
        },
        {
        }
      ],
      "forkastede": [
        {
          "vedtaksperiode": {
            "beregningsdato": "2020-09-01"
          }
        },
        {
          "vedtaksperiode": {
            "beregningsdato": null
          }
        }
      ]
    }
  ],
  "skjemaVersjon": 40
}
"""

@Language("JSON")
private fun expectedJson() =
    """{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "beregningsdato": "2020-09-01",
          "beregningsdatoFraInfotrygd": "2020-09-01"
        },
        {
          "beregningsdato": null,
          "beregningsdatoFraInfotrygd": null
        },
        {
          "beregningsdatoFraInfotrygd": null
        }
      ],
      "forkastede": [
        {
          "vedtaksperiode": {
            "beregningsdato": "2020-09-01",
            "beregningsdatoFraInfotrygd": "2020-09-01"
          }
        },
        {
          "vedtaksperiode": {
            "beregningsdato": null,
            "beregningsdatoFraInfotrygd": null
          }
        }
      ]
    }
  ],
  "skjemaVersjon": 41
}
"""
