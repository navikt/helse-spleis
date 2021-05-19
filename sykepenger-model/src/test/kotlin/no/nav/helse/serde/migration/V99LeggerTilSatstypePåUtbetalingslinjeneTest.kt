package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V99LeggerTilSatstypePåUtbetalingslinjeneTest {
    @Test
    fun `historikk uten utbetalingsperioder`() {
        assertEquals(toNode(expectedJsonIngenUtbetalingsperioder), migrer(originalJsonIngenUtbetalingsperioder))
    }

    private fun toNode(json: String) = serdeObjectMapper.readTree(json)

    private fun migrer(json: String) = listOf(V99LeggerTilSatstypePåUtbetalingslinjene()).migrate(toNode(json))
}

@Language("JSON")
private val originalJsonIngenUtbetalingsperioder = """
{
  "arbeidsgivere": [
    {
      "utbetalinger": [
        {
          "arbeidsgiverOppdrag": {
            "linjer": [
              {},
              {}
            ]
          }
        }
      ]
    },
    {
      "utbetalinger": [
        {
          "arbeidsgiverOppdrag": {
            "linjer": [
              {}
            ]
          }
        },
        {
          "arbeidsgiverOppdrag": {
            "linjer": [
              {}
            ]
          }
        }
      ]
    }
  ]
}
"""

@Language("JSON")
private val expectedJsonIngenUtbetalingsperioder = """
{
  "arbeidsgivere": [
    {
      "utbetalinger": [
        {
          "arbeidsgiverOppdrag": {
            "linjer": [
              {
                "satstype": "DAG"
              },
              {
                "satstype": "DAG"
              }
            ]
          }
        }
      ]
    },
    {
      "utbetalinger": [
        {
          "arbeidsgiverOppdrag": {
            "linjer": [
              {
                "satstype": "DAG"
              }
            ]
          }
        },
        {
          "arbeidsgiverOppdrag": {
            "linjer": [
              {
                "satstype": "DAG"
              }
            ]
          }
        }
      ]
    }
  ],
  "skjemaVersjon": 99
}
"""
