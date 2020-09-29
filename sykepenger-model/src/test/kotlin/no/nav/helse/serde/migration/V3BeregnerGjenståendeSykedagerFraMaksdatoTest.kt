package no.nav.helse.serde.migration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class V3BeregnerGjenståendeSykedagerFraMaksdatoTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `beregner gjenstående sykedager for flere vedtaksperioder`() {
        val json = objectMapper.readTree(personJson)
        listOf(V3BeregnerGjenståendeSykedagerFraMaksdato()).migrate(json)

        val gjenståendeSykedager1 =
            json["arbeidsgivere"].flatMap { it["vedtaksperioder"] }.map { it["gjenståendeSykedager"] }[0]
        val gjenståendeSykedager2 =
            json["arbeidsgivere"].flatMap { it["vedtaksperioder"] }.map { it["gjenståendeSykedager"] }[1]
        val gjenståendeSykedager3 =
            json["arbeidsgivere"].flatMap { it["vedtaksperioder"] }.map { it["gjenståendeSykedager"] }[2]
        val gjenståendeSykedager4 =
            json["arbeidsgivere"].flatMap { it["vedtaksperioder"] }.map { it["gjenståendeSykedager"] }[3]

        assertEquals(24, gjenståendeSykedager1.intValue())
        assertEquals(20, gjenståendeSykedager2.intValue())
        assertTrue(gjenståendeSykedager3.isNull)
        assertTrue(gjenståendeSykedager4.isNull)
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
                "type": "ArbeidsgiverperiodeDag",
                "dato": "2020-01-01"
              },
              {
                "type": "NavDag",
                "dato": "2020-01-02"
              },
              {
                "type": "NavDag",
                "dato": "2020-01-03"
              },
              {
                "type": "NavHelgDag",
                "dato": "2020-01-04"
              },
              {
                "type": "NavHelgDag",
                "dato": "2020-01-05"
              },
              {
                "type": "NavDag",
                "dato": "2020-01-06"
              },
              {
                "type": "NavDag",
                "dato": "2020-01-07"
              },
              {
                "type": "NavDag",
                "dato": "2020-01-08"
              },
              {
                "type": "NavDag",
                "dato": "2020-01-09"
              },
              {
                "type": "NavDag",
                "dato": "2020-01-10"
              },
              {
                "type": "NavHelgDag",
                "dato": "2020-01-11"
              },
              {
                "type": "NavHelgDag",
                "dato": "2020-01-12"
              },
              {
                "type": "NavDag",
                "dato": "2020-01-13"
              }
            ]
          }
        },
        {
          "utbetalingstidslinje": {
            "dager": [
              {
                "type": "ArbeidsgiverperiodeDag",
                "dato": "2020-01-01"
              },
              {
                "type": "NavDag",
                "dato": "2020-01-02"
              },
              {
                "type": "NavDag",
                "dato": "2020-01-03"
              },
              {
                "type": "NavHelgDag",
                "dato": "2020-01-04"
              },
              {
                "type": "NavHelgDag",
                "dato": "2020-01-05"
              },
              {
                "type": "NavDag",
                "dato": "2020-01-06"
              }
            ]
          }
        }
      ],
      "vedtaksperioder": [
        {
          "maksdato": "2020-02-07",
          "forbrukteSykedager": 100,
          "sykdomshistorikk": [
            {
              "beregnetSykdomstidslinje": [
                {
                  "dagen": "2020-01-01",
                  "type": "SYKEDAG_SØKNAD",
                  "grad": 100.0
                },
                {
                  "dagen": "2020-01-02",
                  "type": "SYKEDAG_SØKNAD",
                  "grad": 100.0
                },
                {
                  "dagen": "2020-01-03",
                  "type": "SYKEDAG_SØKNAD",
                  "grad": 100.0
                },
                {
                  "dagen": "2020-01-04",
                  "type": "SYK_HELGEDAG_SØKNAD",
                  "grad": 100.0
                },
                {
                  "dagen": "2020-01-05",
                  "type": "SYK_HELGEDAG_SØKNAD",
                  "grad": 100.0
                },
                {
                  "dagen": "2020-01-06",
                  "type": "SYKEDAG_SØKNAD",
                  "grad": 100.0
                }
              ]
            }
          ]
        },
        {
          "maksdato": "2020-02-07",
          "forbrukteSykedager": 100,
          "sykdomshistorikk": [
            {
              "beregnetSykdomstidslinje": [
                {
                  "dagen": "2020-01-07",
                  "type": "SYKEDAG_SØKNAD",
                  "grad": 100.0
                },
                {
                  "dagen": "2020-01-08",
                  "type": "SYKEDAG_SØKNAD",
                  "grad": 100.0
                },
                {
                  "dagen": "2020-01-09",
                  "type": "SYKEDAG_SØKNAD",
                  "grad": 100.0
                },
                {
                  "dagen": "2020-01-10",
                  "type": "SYKEDAG_SØKNAD",
                  "grad": 100.0
                },
                {
                  "dagen": "2020-01-11",
                  "type": "SYK_HELGEDAG_SØKNAD",
                  "grad": 100.0
                }
              ]
            }
          ]
        },
        {
          "maksdato": null,
          "forbrukteSykedager": 100,
          "sykdomshistorikk": [
            {
              "beregnetSykdomstidslinje": [
                {
                  "dagen": "2020-01-12",
                  "type": "SYK_HELGEDAG_SØKNAD",
                  "grad": 100.0
                },
                {
                  "dagen": "2020-01-13",
                  "type": "SYKEDAG_SØKNAD",
                  "grad": 100.0
                }
              ]
            }
          ]
        },
        {
          "maksdato": null,
          "forbrukteSykedager": 0,
          "sykdomshistorikk": [
            {
              "beregnetSykdomstidslinje": [
                {
                  "dagen": "2020-08-03",
                  "type": "SYKEDAG_SØKNAD",
                  "grad": 100.0
                },
                {
                  "dagen": "2020-08-04",
                  "type": "SYKEDAG_SØKNAD",
                  "grad": 100.0
                },
                {
                  "dagen": "2020-08-05",
                  "type": "SYKEDAG_SØKNAD",
                  "grad": 100.0
                },
                {
                  "dagen": "2020-08-06",
                  "type": "SYKEDAG_SØKNAD",
                  "grad": 100.0
                },
                {
                  "dagen": "2020-08-07",
                  "type": "SYKEDAG_SØKNAD",
                  "grad": 100.0
                }
              ]
            }
          ]
        }
      ]
    }
  ]
}
"""
