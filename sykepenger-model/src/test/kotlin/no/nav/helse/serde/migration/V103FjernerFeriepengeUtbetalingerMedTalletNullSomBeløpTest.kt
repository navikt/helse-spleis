package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V103FjernerFeriepengeUtbetalingerMedTalletNullSomBeløpTest {
    @Test
    fun `Fjerner feriepengeutbetaling med 0 i beløp som er sendt til oppdrag`() {
        assertEquals(toNode(expected), migrer(original))
    }

    @Test
    fun `Fjerner ikke feriepengeutbetaling med 0 i beløp som ikke er sendt til oppdrag`() {
        assertEquals(toNode(expectedMedIkkeSendt), migrer(originalMedIkkeSendt))
    }

    @Test
    fun `Fjerner ikke dersom beløpet er større enn 0`() {
        assertEquals(toNode(expectedMedPositiveBeløp), migrer(originalMedPositiveBeløp))
    }

    @Test
    fun `Fjerner ikke dersom beløpet er negativt`() {
        assertEquals(toNode(expectedMedNegativtBeløp), migrer(originalMedNegativtBeløp))
    }

    @Test
    fun `setter ingenting når det ikke er no feriepengeutbetalinger`() {
        assertEquals(toNode(expectedNoFerieutbetaling), migrer(noFerieutbetaling))
    }

    private fun toNode(json: String) = serdeObjectMapper.readTree(json)

    private fun migrer(json: String) = listOf(V103FjernerFeriepengeUtbetalingerMedTalletNullSomBeløp()).migrate(toNode(json))
}

@Language("JSON")
private val original = """{
  "arbeidsgivere": [
    {
      "feriepengeutbetalinger": [
        {
          "infotrygdFeriepengebeløpPerson": 0.0,
          "infotrygdFeriepengebeløpArbeidsgiver": 0.0,
          "spleisFeriepengebeløpArbeidsgiver": 6276.366,
          "sendTilOppdrag": true,
          "oppdrag": {
            "nettoBeløp": 6276
          }
        },
        {
          "infotrygdFeriepengebeløpPerson": 0.0,
          "infotrygdFeriepengebeløpArbeidsgiver": 0.0,
          "spleisFeriepengebeløpArbeidsgiver": 0.0,
          "sendTilOppdrag": true,
          "oppdrag": {
            "nettoBeløp": 0
          }
        }
      ]
    }
  ],
  "skjemaVersjon": 102
}
"""

@Language("JSON")
private val expected = """{
  "arbeidsgivere": [
    {
      "feriepengeutbetalinger": [
        {
          "infotrygdFeriepengebeløpPerson": 0.0,
          "infotrygdFeriepengebeløpArbeidsgiver": 0.0,
          "spleisFeriepengebeløpArbeidsgiver": 6276.366,
          "sendTilOppdrag": true,
          "oppdrag": {
            "nettoBeløp": 6276
          }
        }
      ]
    }
  ],
  "skjemaVersjon": 103
}
"""

@Language("JSON")
private val originalMedIkkeSendt = """{
  "arbeidsgivere": [
    {
      "feriepengeutbetalinger": [
        {
          "infotrygdFeriepengebeløpPerson": 0.0,
          "infotrygdFeriepengebeløpArbeidsgiver": 0.0,
          "spleisFeriepengebeløpArbeidsgiver": 6276.366,
          "sendTilOppdrag": true,
          "oppdrag": {
            "nettoBeløp": 6276
          }
        },
        {
          "infotrygdFeriepengebeløpPerson": 0.0,
          "infotrygdFeriepengebeløpArbeidsgiver": 0.0,
          "spleisFeriepengebeløpArbeidsgiver": 0.0,
          "sendTilOppdrag": false,
          "oppdrag": {
            "nettoBeløp": 0
          }
        }
      ]
    }
  ],
  "skjemaVersjon": 102
}
"""

@Language("JSON")
private val expectedMedIkkeSendt = """{
  "arbeidsgivere": [
    {
      "feriepengeutbetalinger": [
        {
          "infotrygdFeriepengebeløpPerson": 0.0,
          "infotrygdFeriepengebeløpArbeidsgiver": 0.0,
          "spleisFeriepengebeløpArbeidsgiver": 6276.366,
          "sendTilOppdrag": true,
          "oppdrag": {
            "nettoBeløp": 6276
          }
        },
        {
          "infotrygdFeriepengebeløpPerson": 0.0,
          "infotrygdFeriepengebeløpArbeidsgiver": 0.0,
          "spleisFeriepengebeløpArbeidsgiver": 0.0,
          "sendTilOppdrag": false,
          "oppdrag": {
            "nettoBeløp": 0
          }
        }
      ]
    }
  ],
  "skjemaVersjon": 103
}
"""

@Language("JSON")
private val originalMedPositiveBeløp = """{
  "arbeidsgivere": [
    {
      "feriepengeutbetalinger": [
        {
          "infotrygdFeriepengebeløpPerson": 0.0,
          "infotrygdFeriepengebeløpArbeidsgiver": 0.0,
          "spleisFeriepengebeløpArbeidsgiver": 6276.366,
          "sendTilOppdrag": true,
          "oppdrag": {
            "nettoBeløp": 6276
          }
        },
        {
          "infotrygdFeriepengebeløpPerson": 0.0,
          "infotrygdFeriepengebeløpArbeidsgiver": 0.0,
          "spleisFeriepengebeløpArbeidsgiver": 0.0,
          "sendTilOppdrag": false,
          "oppdrag": {
            "nettoBeløp": 6277
          }
        }
      ]
    }
  ],
  "skjemaVersjon": 102
}
"""

@Language("JSON")
private val expectedMedPositiveBeløp = """{
  "arbeidsgivere": [
    {
      "feriepengeutbetalinger": [
        {
          "infotrygdFeriepengebeløpPerson": 0.0,
          "infotrygdFeriepengebeløpArbeidsgiver": 0.0,
          "spleisFeriepengebeløpArbeidsgiver": 6276.366,
          "sendTilOppdrag": true,
          "oppdrag": {
            "nettoBeløp": 6276
          }
        },
        {
          "infotrygdFeriepengebeløpPerson": 0.0,
          "infotrygdFeriepengebeløpArbeidsgiver": 0.0,
          "spleisFeriepengebeløpArbeidsgiver": 0.0,
          "sendTilOppdrag": false,
          "oppdrag": {
            "nettoBeløp": 6277
          }
        }
      ]
    }
  ],
  "skjemaVersjon": 103
}
"""

@Language("JSON")
private val originalMedNegativtBeløp = """{
  "arbeidsgivere": [
    {
      "feriepengeutbetalinger": [
        {
          "infotrygdFeriepengebeløpPerson": 0.0,
          "infotrygdFeriepengebeløpArbeidsgiver": 0.0,
          "spleisFeriepengebeløpArbeidsgiver": 6276.366,
          "sendTilOppdrag": true,
          "oppdrag": {
            "nettoBeløp": 6276
          }
        },
        {
          "infotrygdFeriepengebeløpPerson": 0.0,
          "infotrygdFeriepengebeløpArbeidsgiver": 0.0,
          "spleisFeriepengebeløpArbeidsgiver": 0.0,
          "sendTilOppdrag": false,
          "oppdrag": {
            "nettoBeløp": -6277
          }
        }
      ]
    }
  ],
  "skjemaVersjon": 102
}
"""

@Language("JSON")
private val expectedMedNegativtBeløp = """{
  "arbeidsgivere": [
    {
      "feriepengeutbetalinger": [
        {
          "infotrygdFeriepengebeløpPerson": 0.0,
          "infotrygdFeriepengebeløpArbeidsgiver": 0.0,
          "spleisFeriepengebeløpArbeidsgiver": 6276.366,
          "sendTilOppdrag": true,
          "oppdrag": {
            "nettoBeløp": 6276
          }
        },
        {
          "infotrygdFeriepengebeløpPerson": 0.0,
          "infotrygdFeriepengebeløpArbeidsgiver": 0.0,
          "spleisFeriepengebeløpArbeidsgiver": 0.0,
          "sendTilOppdrag": false,
          "oppdrag": {
            "nettoBeløp": -6277
          }
        }
      ]
    }
  ],
  "skjemaVersjon": 103
}
"""


@Language("JSON")
private val noFerieutbetaling = """{
  "arbeidsgivere": [
    {
    }
  ],
  "skjemaVersjon": 102
}
"""

@Language("JSON")
private val expectedNoFerieutbetaling = """{
  "arbeidsgivere": [
    {
    }
  ],
  "skjemaVersjon": 103
}
"""
