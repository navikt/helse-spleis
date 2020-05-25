package no.nav.helse.serde.migration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V15ØkonomiSykdomstidslinjerTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `hver Dag er satt til 100 prosent arbeidsgiverutbetaling`() {
        val json = objectMapper.readTree(personJson)
        listOf(V15ØkonomiSykdomstidslinjer()).migrate(json)
        val migratedJson = objectMapper.readTree(json.toString())
        val expected = objectMapper.readTree(expectedPersonJson)
        assertEquals(expected, migratedJson)
    }
}

@Language("JSON")
private const val personJson = """{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "sykdomshistorikk": [
            {
              "hendelseSykdomstidslinje": {
                "dager": [
                  {
                    "grad": 45.0
                  },
                  {
                    "grad": 100.0
                  }
                ]
              },
              "beregnetSykdomstidslinje": {
                "dager": [
                  {
                    "grad": 60.0
                  },
                  {
                    "grad": 100.0
                  }
                ]
              }
            },
            {
              "hendelseSykdomstidslinje": {
                "dager": [
                  {
                    "grad": 80.0
                  },
                  {
                    "grad": 100.0
                  }
                ]
              },
              "beregnetSykdomstidslinje": {
                "dager": [
                  {
                    "grad": 75.0
                  },
                  {
                    "grad": 100.0
                  }
                ]
              }
            }
          ]
        }
      ]
    }
  ],
  "skjemaVersjon": 14
}
"""

@Language("JSON")
private const val expectedPersonJson = """{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "sykdomshistorikk": [
            {
              "hendelseSykdomstidslinje": {
                "dager": [
                  {
                    "grad": 45.0,
                    "arbeidsgiverBetalingProsent": 100.0
                  },
                  {
                    "grad": 100.0,
                    "arbeidsgiverBetalingProsent": 100.0
                  }
                ]
              },
              "beregnetSykdomstidslinje": {
                "dager": [
                  {
                    "grad": 60.0,
                    "arbeidsgiverBetalingProsent": 100.0
                  },
                  {
                    "grad": 100.0,
                    "arbeidsgiverBetalingProsent": 100.0
                  }
                ]
              }
            },
            {
              "hendelseSykdomstidslinje": {
                "dager": [
                  {
                    "grad": 80.0,
                    "arbeidsgiverBetalingProsent": 100.0
                  },
                  {
                    "grad": 100.0,
                    "arbeidsgiverBetalingProsent": 100.0
                  }
                ]
              },
              "beregnetSykdomstidslinje": {
                "dager": [
                  {
                    "grad": 75.0,
                    "arbeidsgiverBetalingProsent": 100.0
                  },
                  {
                    "grad": 100.0,
                    "arbeidsgiverBetalingProsent": 100.0
                  }
                ]
              }
            }
          ]
        }
      ]
    }
  ],
  "skjemaVersjon": 15
}
"""
