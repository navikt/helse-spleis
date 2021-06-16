package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V104SlettOpphørMedFeilKlassekodeTest {
    @Test
    fun `Fjerner utbetalinger dersom opphør og feil klassekode`() {
        assertEquals(toNode(expected), migrer(original))
    }

    private fun toNode(json: String) = serdeObjectMapper.readTree(json)

    private fun migrer(json: String) = listOf(V104SlettOpphørMedFeilKlassekode()).migrate(toNode(json))
}

@Language("JSON")
private val original = """
   {
      "arbeidsgivere": [
        {
          "feriepengeutbetalinger": [
            {
              "oppdrag": {
                "linjer": [
                  {
                    "statuskode": null,
                    "klassekode": "SPREFAGFER-IOP"
                  }
                ]
              }
            },
            {
              "oppdrag": {
                "linjer": [
                  {
                    "statuskode": "OPPH",
                    "klassekode": "SPREFAG-IOP"
                  }
                ]
              }
            },
            {
              "oppdrag": {
                "linjer": []
              }
            }
          ]
        }
      ],
      "skjemaVersjon": 103
  }
"""

@Language("JSON")
private val expected = """
   {
      "arbeidsgivere": [
        {
          "feriepengeutbetalinger": [
            {
              "oppdrag": {
                "linjer": [
                  {
                    "statuskode": null,
                    "klassekode": "SPREFAGFER-IOP"
                  }
                ]
              }
            }
          ]
        }
      ],
      "skjemaVersjon": 104
  }
"""
