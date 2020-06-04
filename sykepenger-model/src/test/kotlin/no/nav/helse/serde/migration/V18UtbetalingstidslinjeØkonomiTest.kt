package no.nav.helse.serde.migration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V18UtbetalingstidslinjeØkonomiTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `befolke økonomifelt i Utbetalalingstidslinjer`() {
        val json = objectMapper.readTree(personJson)
        listOf(V18UtbetalingstidslinjeØkonomi()).migrate(json)
        val migratedJson = objectMapper.readTree(json.toString())
        val expected = objectMapper.readTree(expectedPersonJson)
        assertEquals(expected, migratedJson)
    }
}

@Language("JSON")
private const val personJson = """{
  "arbeidsgivere": [
    {
      "utbetalinger": [
        {
          "utbetalingstidslinje": {
            "dager": [
              {
                "type": "ArbeidsgiverperiodeDag",
                "dagsats": 1431,
                "dato": "2018-01-01"
              },
              {
                "type": "NavDag",
                "dagsats": 1431,
                "dato": "2018-01-17",
                "utbetaling": 1431,
                "grad": 100.0
              },
              {
                "type": "NavDag",
                "dagsats": 1431,
                "dato": "2018-01-18",
                "utbetaling": 1431,
                "grad": 100.0
              },
              {
                "type": "NavHelgDag",
                "dagsats": 0,
                "dato": "2018-01-20",
                "grad": 100.0
              },
              {
                "type": "Arbeidsdag",
                "dagsats": 1431,
                "dato": "2018-01-21"
              },
              {
                "type": "Fridag",
                "dagsats": 0,
                "dato": "2018-01-22"
              }
            ]
          }
        },
        {
          "utbetalingstidslinje": {
            "dager": [
              {
                "type": "ArbeidsgiverperiodeDag",
                "dagsats": 1431,
                "dato": "2018-01-01"
              },
              {
                "type": "NavDag",
                "dagsats": 1431,
                "dato": "2018-01-17",
                "utbetaling": 1431,
                "grad": 100.0
              },
              {
                "type": "NavDag",
                "dagsats": 1431,
                "dato": "2018-01-18",
                "utbetaling": 1431,
                "grad": 100.0
              },
              {
                "type": "NavHelgDag",
                "dagsats": 0,
                "dato": "2018-01-20",
                "grad": 100.0
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
                "type": "ArbeidsgiverperiodeDag",
                "dagsats": 1431,
                "dato": "2018-01-01"
              },
              {
                "type": "NavDag",
                "dagsats": 1431,
                "dato": "2018-01-17",
                "utbetaling": 1431,
                "grad": 100.0
              },
              {
                "type": "NavDag",
                "dagsats": 1431,
                "dato": "2018-01-18",
                "utbetaling": 1431,
                "grad": 100.0
              },
              {
                "type": "NavHelgDag",
                "dagsats": 0,
                "dato": "2018-01-20",
                "grad": 100.0
              }
            ]
          }
        }
      ],
      "forkastede": [
        {
          "utbetalingstidslinje": {
            "dager": [
              {
                "type": "ArbeidsgiverperiodeDag",
                "dagsats": 1431,
                "dato": "2018-01-01"
              },
              {
                "type": "NavDag",
                "dagsats": 1431,
                "dato": "2018-01-17",
                "utbetaling": 1431,
                "grad": 100.0
              },
              {
                "type": "NavDag",
                "dagsats": 1431,
                "dato": "2018-01-18",
                "utbetaling": 1431,
                "grad": 100.0
              },
              {
                "type": "NavHelgDag",
                "dagsats": 0,
                "dato": "2018-01-20",
                "grad": 100.0
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
private const val expectedPersonJson = """{
  "arbeidsgivere": [
    {
      "utbetalinger": [
        {
          "utbetalingstidslinje": {
            "dager": [
              {
                "type": "ArbeidsgiverperiodeDag",
                "dato": "2018-01-01",
                "grad": 0.0,
                "arbeidsgiverBetalingProsent": 100.0,
                "dekningsgrunnlag": 1431.0,
                "aktuellDagsinntekt": 1431.0,
                "arbeidsgiverbeløp": 0,
                "personbeløp": 0,
                "er6GBegrenset": false
              },
              {
                "type": "NavDag",
                "dato": "2018-01-17",
                "grad": 100.0,
                "arbeidsgiverBetalingProsent": 100.0,
                "dekningsgrunnlag": 1431.0,
                "aktuellDagsinntekt": 1431.0,
                "arbeidsgiverbeløp": 1431,
                "personbeløp": 0,
                "er6GBegrenset": false
              },
              {
                "type": "NavDag",
                "dato": "2018-01-18",
                "grad": 100.0,
                "arbeidsgiverBetalingProsent": 100.0,
                "dekningsgrunnlag": 1431.0,
                "aktuellDagsinntekt": 1431.0,
                "arbeidsgiverbeløp": 1431,
                "personbeløp": 0,
                "er6GBegrenset": false
              },
              {
                "type": "NavHelgDag",
                "dato": "2018-01-20",
                "grad": 100.0,
                "arbeidsgiverBetalingProsent": 100.0,
                "dekningsgrunnlag": 0.0,
                "aktuellDagsinntekt": 0.0,
                "arbeidsgiverbeløp": 0,
                "personbeløp": 0,
                "er6GBegrenset": false
              },
              {
                "type": "Arbeidsdag",
                "dato": "2018-01-21",
                "grad": 0.0,
                "arbeidsgiverBetalingProsent": 100.0,
                "dekningsgrunnlag": 1431.0,
                "aktuellDagsinntekt": 1431.0,
                "arbeidsgiverbeløp": 0,
                "personbeløp": 0,
                "er6GBegrenset": false
              },
              {
                "type": "Fridag",
                "dato": "2018-01-22",
                "grad": 0.0,
                "arbeidsgiverBetalingProsent": 100.0,
                "dekningsgrunnlag": 1431.0,
                "aktuellDagsinntekt": 1431.0,
                "arbeidsgiverbeløp": 0,
                "personbeløp": 0,
                "er6GBegrenset": false
              }
            ]
          }
        },
        {
          "utbetalingstidslinje": {
            "dager": [
              {
                "type": "ArbeidsgiverperiodeDag",
                "dato": "2018-01-01",
                "grad": 0.0,
                "arbeidsgiverBetalingProsent": 100.0,
                "dekningsgrunnlag": 1431.0,
                "aktuellDagsinntekt": 1431.0,
                "arbeidsgiverbeløp": 0,
                "personbeløp": 0,
                "er6GBegrenset": false
              },
              {
                "type": "NavDag",
                "dato": "2018-01-17",
                "grad": 100.0,
                "arbeidsgiverBetalingProsent": 100.0,
                "dekningsgrunnlag": 1431.0,
                "aktuellDagsinntekt": 1431.0,
                "arbeidsgiverbeløp": 1431,
                "personbeløp": 0,
                "er6GBegrenset": false
              },
              {
                "type": "NavDag",
                "dato": "2018-01-18",
                "grad": 100.0,
                "arbeidsgiverBetalingProsent": 100.0,
                "dekningsgrunnlag": 1431.0,
                "aktuellDagsinntekt": 1431.0,
                "arbeidsgiverbeløp": 1431,
                "personbeløp": 0,
                "er6GBegrenset": false
              },
              {
                "type": "NavHelgDag",
                "dato": "2018-01-20",
                "grad": 100.0,
                "arbeidsgiverBetalingProsent": 100.0,
                "dekningsgrunnlag": 0.0,
                "aktuellDagsinntekt": 0.0,
                "arbeidsgiverbeløp": 0,
                "personbeløp": 0,
                "er6GBegrenset": false
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
                "type": "ArbeidsgiverperiodeDag",
                "dato": "2018-01-01",
                "grad": 0.0,
                "arbeidsgiverBetalingProsent": 100.0,
                "dekningsgrunnlag": 1431.0,
                "aktuellDagsinntekt": 1431.0,
                "arbeidsgiverbeløp": 0,
                "personbeløp": 0,
                "er6GBegrenset": false
              },
              {
                "type": "NavDag",
                "dato": "2018-01-17",
                "grad": 100.0,
                "arbeidsgiverBetalingProsent": 100.0,
                "dekningsgrunnlag": 1431.0,
                "aktuellDagsinntekt": 1431.0,
                "arbeidsgiverbeløp": 1431,
                "personbeløp": 0,
                "er6GBegrenset": false
              },
              {
                "type": "NavDag",
                "dato": "2018-01-18",
                "grad": 100.0,
                "arbeidsgiverBetalingProsent": 100.0,
                "dekningsgrunnlag": 1431.0,
                "aktuellDagsinntekt": 1431.0,
                "arbeidsgiverbeløp": 1431,
                "personbeløp": 0,
                "er6GBegrenset": false
              },
              {
                "type": "NavHelgDag",
                "dato": "2018-01-20",
                "grad": 100.0,
                "arbeidsgiverBetalingProsent": 100.0,
                "dekningsgrunnlag": 0.0,
                "aktuellDagsinntekt": 0.0,
                "arbeidsgiverbeløp": 0,
                "personbeløp": 0,
                "er6GBegrenset": false
              }
            ]
          }
        }
      ],
      "forkastede": [
        {
          "utbetalingstidslinje": {
            "dager": [
              {
                "type": "ArbeidsgiverperiodeDag",
                "dato": "2018-01-01",
                "grad": 0.0,
                "arbeidsgiverBetalingProsent": 100.0,
                "dekningsgrunnlag": 1431.0,
                "aktuellDagsinntekt": 1431.0,
                "arbeidsgiverbeløp": 0,
                "personbeløp": 0,
                "er6GBegrenset": false
              },
              {
                "type": "NavDag",
                "dato": "2018-01-17",
                "grad": 100.0,
                "arbeidsgiverBetalingProsent": 100.0,
                "dekningsgrunnlag": 1431.0,
                "aktuellDagsinntekt": 1431.0,
                "arbeidsgiverbeløp": 1431,
                "personbeløp": 0,
                "er6GBegrenset": false
              },
              {
                "type": "NavDag",
                "dato": "2018-01-18",
                "grad": 100.0,
                "arbeidsgiverBetalingProsent": 100.0,
                "dekningsgrunnlag": 1431.0,
                "aktuellDagsinntekt": 1431.0,
                "arbeidsgiverbeløp": 1431,
                "personbeløp": 0,
                "er6GBegrenset": false
              },
              {
                "type": "NavHelgDag",
                "dato": "2018-01-20",
                "grad": 100.0,
                "arbeidsgiverBetalingProsent": 100.0,
                "dekningsgrunnlag": 0.0,
                "aktuellDagsinntekt": 0.0,
                "arbeidsgiverbeløp": 0,
                "personbeløp": 0,
                "er6GBegrenset": false
              }
            ]
          }
        }
      ]
    }
  ],
  "skjemaVersjon": 18
}
"""
