package no.nav.helse.serde.migration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V21FjernGruppeIdTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `Fjerner gruppeId`() {
        val json = objectMapper.readTree(personJson)
        listOf(V21FjernGruppeId()).migrate(json)
        val migratedJson = objectMapper.readTree(json.toString())
        val expected = objectMapper.readTree(expectedPersonJson)
        assertEquals(expected, migratedJson)
    }
}

@Language("JSON")
private const val personJson = """
{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "gruppeId": "00000000-0000-0000-0000-000000000003"
        }
      ],
      "forkastede": [
        {
          "gruppeId": "00000000-0000-0000-0000-000000000004"
        }
      ]
    }
  ]
}
"""

@Language("JSON")
private const val expectedPersonJson = """
{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
        }
      ],
      "forkastede": [
        {
        }
      ]
    }
  ],
  "skjemaVersjon": 21
}
"""
