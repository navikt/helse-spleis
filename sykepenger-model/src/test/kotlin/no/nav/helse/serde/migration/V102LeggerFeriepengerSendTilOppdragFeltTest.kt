package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V102LeggerFeriepengerSendTilOppdragFeltTest {
    @Test
    fun `setter sendTilOppdrag til sant når oppdragsummen er ulik null`() {
        assertEquals(toNode(expected), migrer(original))
    }

    @Test
    fun `setter sendTilOppdrag til sant når oppdragsummen er negativ`() {
        assertEquals(toNode(expectedMedNegativtBeløp), migrer(originalMedNegativtBeløp))
    }

    @Test
    fun `setter sendTilOppdrag til ikke sant når oppdragsummen er lik null`() {
        assertEquals(toNode(expectedMedBeløpLikIngenting), migrer(originalMedBeløpLikIngenting))
    }

    @Test
    fun `setter ingenting når det ikke er no feriepengeutbetalinger`() {
        assertEquals(toNode(expectedNoFerieutbetaling), migrer(noFerieutbetaling))
    }

    private fun toNode(json: String) = serdeObjectMapper.readTree(json)

    private fun migrer(json: String) = listOf(V102LeggerFeriepengerSendTilOppdragFelt()).migrate(toNode(json))
}

@Language("JSON")
private val original = """{
  "arbeidsgivere": [
    {
      "feriepengeutbetalinger": [
        {
          "utbetalingId": "532c9b3b-8987-4065-831b-cfe6cb61a8f4",
          "opptjeningsår": "2018",
          "oppdrag": {
            "nettoBeløp": 1606
          }
        }
      ]
    }
  ],
  "skjemaVersjon": 101
}
"""

@Language("JSON")
private val expected = """{
  "arbeidsgivere": [
    {
      "feriepengeutbetalinger": [
        {
          "utbetalingId": "532c9b3b-8987-4065-831b-cfe6cb61a8f4",
          "opptjeningsår": "2018",
          "oppdrag": {
            "nettoBeløp": 1606
          },
          "sendTilOppdrag": true
        }
      ]
    }
  ],
  "skjemaVersjon": 102
}

"""
@Language("JSON")
private val originalMedNegativtBeløp = """{
  "arbeidsgivere": [
    {
      "feriepengeutbetalinger": [
        {
          "utbetalingId": "532c9b3b-8987-4065-831b-cfe6cb61a8f4",
          "opptjeningsår": "2018",
          "oppdrag": {
            "nettoBeløp": -1606
          }
        }
      ]
    }
  ],
  "skjemaVersjon": 101
}
"""

@Language("JSON")
private val expectedMedNegativtBeløp = """{
  "arbeidsgivere": [
    {
      "feriepengeutbetalinger": [
        {
          "utbetalingId": "532c9b3b-8987-4065-831b-cfe6cb61a8f4",
          "opptjeningsår": "2018",
          "oppdrag": {
            "nettoBeløp": -1606
          },
          "sendTilOppdrag": true
        }
      ]
    }
  ],
  "skjemaVersjon": 102
}
"""

@Language("JSON")
private val originalMedBeløpLikIngenting = """{
  "arbeidsgivere": [
    {
      "feriepengeutbetalinger": [
        {
          "utbetalingId": "532c9b3b-8987-4065-831b-cfe6cb61a8f4",
          "opptjeningsår": "2018",
          "oppdrag": {
            "nettoBeløp": 0
          }
        }
      ]
    }
  ],
  "skjemaVersjon": 101
}
"""

@Language("JSON")
private val expectedMedBeløpLikIngenting = """{
  "arbeidsgivere": [
    {
      "feriepengeutbetalinger": [
        {
          "utbetalingId": "532c9b3b-8987-4065-831b-cfe6cb61a8f4",
          "opptjeningsår": "2018",
          "oppdrag": {
            "nettoBeløp": 0
          },
          "sendTilOppdrag": false
        }
      ]
    }
  ],
  "skjemaVersjon": 102
}
"""

@Language("JSON")
private val noFerieutbetaling = """{
  "arbeidsgivere": [
    {
    }
  ],
  "skjemaVersjon": 101
}
"""

@Language("JSON")
private val expectedNoFerieutbetaling = """{
  "arbeidsgivere": [
    {
    }
  ],
  "skjemaVersjon": 102
}
"""
