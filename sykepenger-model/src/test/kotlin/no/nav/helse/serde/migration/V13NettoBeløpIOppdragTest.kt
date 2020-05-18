package no.nav.helse.serde.migration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V13NettoBeløpIOppdragTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `setter netto beløp til 0 som default`() {
        val json = objectMapper.readTree(personJson)
        listOf(
            V13NettoBeløpIOppdrag()
        ).migrate(json)
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
      "utbetalinger": [
        {
          "arbeidsgiverOppdrag": {},
          "personOppdrag": {}
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
      "utbetalinger": [
        {
          "arbeidsgiverOppdrag": {
            "nettoBeløp": 0
          },
          "personOppdrag": {
            "nettoBeløp": 0
          }
        }
      ]
    }
  ],
  "skjemaVersjon": 13
}
"""
