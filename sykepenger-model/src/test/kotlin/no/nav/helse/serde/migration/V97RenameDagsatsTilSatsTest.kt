package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V97RenameDagsatsTilSatsTest {
    @Test
    fun `Endrer navn fra dagsats til sats`() {
        assertEquals(toNode(expectedJson), migrer(originalJson))
    }

    private fun toNode(json: String) = serdeObjectMapper.readTree(json)

    private fun migrer(json: String) = listOf(V97RenameDagsatsTilSats())
        .migrate(toNode(json))
}
@Language("JSON")
private val originalJson = """
{
  "arbeidsgivere": [
    {
      "utbetalinger": [
        {
          "arbeidsgiverOppdrag": {
            "linjer": [
              {
                "dagsats": 1337
              }
            ]
          },
          "personOppdrag": {
            "linjer": [
              {
                "dagsats": 420
              }
            ]
          }
        }
      ]
    }
  ]
}

"""

@Language("JSON")
private val expectedJson = """
{
  "arbeidsgivere": [
    {
      "utbetalinger": [
        {
          "arbeidsgiverOppdrag": {
            "linjer": [
              {
                "sats": 1337
              }
            ]
          },
          "personOppdrag": {
            "linjer": [
              {
                "sats": 420
              }
            ]
          }
        }
      ]
    }
  ],
  "skjemaVersjon": 97
}

"""
