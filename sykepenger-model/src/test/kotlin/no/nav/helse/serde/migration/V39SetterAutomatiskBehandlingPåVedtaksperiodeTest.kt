package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class V39SetterAutomatiskBehandlingPåVedtaksperiodeTest {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `Setter automatiskBehandling til false for godkjente perioder og null ellers`() {
        val migrated = listOf(V39SetterAutomatiskBehandlingPåVedtaksperiode())
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
          "godkjentAv": "Saksbehandler"
        },
        {

        },
        {
          "godkjentAv": null
        }
      ],
      "forkastede": [
        {
          "vedtaksperiode": {
            "godkjentAv": "Saksbehandler"
          }
        },
        {
          "vedtaksperiode": {
            "godkjentAv": null
          }
        }
      ]
    }
  ],
  "skjemaVersjon": 38
}
"""

@Language("JSON")
private fun expectedJson() =
    """{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "godkjentAv": "Saksbehandler",
          "automatiskBehandling": false
        },
        {
          "automatiskBehandling": null
        },
        {
          "godkjentAv": null,
          "automatiskBehandling": null
        }
      ],
      "forkastede": [
        {
          "vedtaksperiode": {
            "godkjentAv": "Saksbehandler",
            "automatiskBehandling": false
          }
        },
        {
          "vedtaksperiode": {
            "godkjentAv": null,
            "automatiskBehandling": null
          }
        }
      ]
    }
  ],
  "skjemaVersjon": 39
}
"""
