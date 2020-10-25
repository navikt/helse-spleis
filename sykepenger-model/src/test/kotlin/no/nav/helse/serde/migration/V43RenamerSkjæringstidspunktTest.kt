package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class V43RenamerSkjæringstidspunktTest {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `renamer beregningsdato til beregningsdatoFraInfotrygd`() {
        val migrated = listOf(V43RenamerSkjæringstidspunkt())
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
          "beregningsdatoFraInfotrygd": "2020-09-01"
        },
        {
          "beregningsdatoFraInfotrygd": null
        },
        {
        }
      ],
      "forkastede": [
        {
          "beregningsdatoFraInfotrygd": "2020-09-01"
        },
        {
          "beregningsdatoFraInfotrygd": null
        }
      ]
    }
  ],
  "skjemaVersjon": 42
}
"""

@Language("JSON")
private fun expectedJson() =
    """{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "beregningsdatoFraInfotrygd": "2020-09-01",
          "skjæringstidspunktFraInfotrygd": "2020-09-01"
        },
        {
          "beregningsdatoFraInfotrygd": null,
          "skjæringstidspunktFraInfotrygd": null
        },
        {
          "skjæringstidspunktFraInfotrygd": null
        }
      ],
      "forkastede": [
        {
          "beregningsdatoFraInfotrygd": "2020-09-01",
          "skjæringstidspunktFraInfotrygd": "2020-09-01"
        },
        {
          "beregningsdatoFraInfotrygd": null,
          "skjæringstidspunktFraInfotrygd": null
        }
      ]
    }
  ],
  "skjemaVersjon": 43
}
"""
