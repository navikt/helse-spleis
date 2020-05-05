package no.nav.helse.serde.migration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V7DagsatsSomHeltallTest {

    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `konverterer inntekt til dagsats`() {
        val json = objectMapper.readTree(personJson)
        listOf(V7DagsatsSomHeltall()).migrate(json)

        assertEquals(1000, json
            .path("arbeidsgivere")[0]
            .path("utbetalinger")[0]
            .path("utbetalingstidslinje")
            .path("dager")[0]
            .path("dagsats")
            .intValue())
        assertEquals(999, json
            .path("arbeidsgivere")[0]
            .path("utbetalinger")[1]
            .path("utbetalingstidslinje")
            .path("dager")[0]
            .path("dagsats")
            .intValue())
        assertEquals(999, json
            .path("arbeidsgivere")[0]
            .path("vedtaksperioder")[0]
            .path("utbetalingstidslinje")
            .path("dager")[0]
            .path("dagsats")
            .intValue())
    }
}

@Language("JSON")
private const val personJson = """
{
  "arbeidsgivere": [
    {
      "utbetalinger": [
        {
          "utbetalingstidslinje": {
            "dager": [
              {
                "inntekt": "999.5"
              }
            ]
          }
        },
        {
          "utbetalingstidslinje": {
            "dager": [
              {
                "inntekt": "999.4"
              }
            ]
          }
        }
      ],
      "vedtaksperioder": [
        {
          "utbetalingstidslinje": {
            "dager": [
              {
                "inntekt": "999.4"
              }
            ]
          }
        }
      ]
    }
  ]
}
"""
