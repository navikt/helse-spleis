package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class V1EndreKunArbeidsgiverSykedagEnumTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `oversetter til ny dagtype`() {
        val json = objectMapper.readTree(personJson)
        listOf(V1EndreKunArbeidsgiverSykedagEnum()).migrate(json)
        val migratedJson = objectMapper.readTree(json.toString())

        migratedJson.path("arbeidsgivere")
            .first()
            .path("vedtaksperioder")
            .forEach { periode ->
                periode.path("sykdomshistorikk")
                    .forEach { element ->
                        assertIngenForekomstAv("KUN_ARBEIDSGIVER_SYKEDAG", element.path("beregnetSykdomstidslinje"))
                        assertIngenForekomstAv("KUN_ARBEIDSGIVER_SYKEDAG", element.path("hendelseSykdomstidslinje"))
                    }
            }

        assertTrue(migratedJson.toString().contains("FORELDET_SYKEDAG")) { "Forventet å finne FORELDET_SYKEDAG i JSON:\n${migratedJson.toPrettyString()}" }
    }

    private fun assertIngenForekomstAv(expected: String, node: JsonNode) {
        dager(node).forEach { (dagen, actual) ->
            assertNotEquals(expected, actual) { "$dagen har type $actual" }
        }
    }

    private fun dager(tidslinje: JsonNode) =
        tidslinje
            .map { it["dagen"].asText() to it["type"].asText() }
            .associateBy { it.first }
            .mapValues { it.value.second }
}

@Language("JSON")
private const val personJson = """
{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
            "sykdomshistorikk": [
                {
                    "tidsstempel": "2020-02-20T00:00:00.000000",
                    "hendelseId": "uuid",
                    "hendelseSykdomstidslinje": [
                        {
                            "dagen": "2020-02-01",
                            "type": "KUN_ARBEIDSGIVER_SYKEDAG"
                        }
                    ],
                    "beregnetSykdomstidslinje": [
                        {
                            "dagen": "2020-02-01",
                            "type": "KUN_ARBEIDSGIVER_SYKEDAG"
                        }
                    ]
                }
            ]
        },
        {
          "sykdomshistorikk": [
            {
              "tidsstempel": "2020-02-07T15:43:10.350016",
              "hendelseId": "483c7972-87c9-4f18-8628-489b93da6d3e",
              "hendelseSykdomstidslinje": [
                {
                  "dagen": "2020-01-01",
                  "type": "KUN_ARBEIDSGIVER_SYKEDAG"
                },
                {
                  "dagen": "2020-01-02",
                  "type": "KUN_ARBEIDSGIVER_SYKEDAG"
                },
                {
                  "dagen": "2020-01-03",
                  "type": "KUN_ARBEIDSGIVER_SYKEDAG"
                },
                {
                  "dagen": "2020-01-04",
                  "type": "KUN_ARBEIDSGIVER_SYKEDAG"
                }
              ],
              "beregnetSykdomstidslinje": [
                {
                  "dagen": "2020-01-01",
                  "type": "SYKEDAG_SYKMELDING"
                },
                {
                  "dagen": "2020-01-02",
                  "hendelseType": "Søknad",
                  "type": "SYK_HELGEDAG"
                },
                {
                  "dagen": "2020-01-03",
                  "hendelseType": "Søknad",
                  "type": "SYK_HELGEDAG"
                },
                {
                  "dagen": "2020-01-04",
                  "type": "SYKEDAG_SYKMELDING"
                },
                {
                  "dagen": "2020-01-05",
                  "type": "SYK_HELGEDAG"
                },
                {
                  "dagen": "2020-01-06",
                  "type": "SYK_HELGEDAG"
                }
              ]
            },
            {
              "tidsstempel": "2020-02-07T15:43:10.346894",
              "hendelseId": "3bbc20fb-64d4-4aac-8e51-46ace187eae4",
              "hendelseSykdomstidslinje": [
                {
                  "dagen": "2020-01-01",
                  "type": "SYKEDAG_SØKNAD"
                },
                {
                  "dagen": "2020-01-02",
                  "type": "SYK_HELGEDAG"
                },
                {
                  "dagen": "2020-01-03",
                  "type": "SYK_HELGEDAG"
                },
                {
                  "dagen": "2020-01-04",
                  "type": "SYKEDAG_SØKNAD"
                }
              ],
              "beregnetSykdomstidslinje": [
                {
                  "dagen": "2020-01-01",
                  "type": "SYKEDAG_SYKMELDING"
                },
                {
                  "dagen": "2020-01-02",
                  "hendelseType": "Søknad",
                  "type": "SYK_HELGEDAG"
                },
                {
                  "dagen": "2020-01-03",
                  "hendelseType": "Søknad",
                  "type": "SYK_HELGEDAG"
                },
                {
                  "dagen": "2020-01-04",
                  "type": "SYKEDAG_SYKMELDING"
                },
                {
                  "dagen": "2020-01-05",
                  "type": "SYK_HELGEDAG"
                },
                {
                  "dagen": "2020-01-06",
                  "type": "SYK_HELGEDAG"
                }
              ]
            },
            {
              "tidsstempel": "2020-02-07T15:43:10.345275",
              "hendelseId": "e03ef661-11ab-464a-a0af-05656ad753cd",
              "hendelseSykdomstidslinje": [
                {
                  "dagen": "2020-01-01",
                  "type": "SYKEDAG_SYKMELDING"
                },
                {
                  "dagen": "2020-01-02",
                  "type": "SYK_HELGEDAG"
                },
                {
                  "dagen": "2020-01-03",
                  "type": "SYK_HELGEDAG"
                },
                {
                  "dagen": "2020-01-04",
                  "type": "SYKEDAG_SYKMELDING"
                },
                {
                  "dagen": "2020-01-05",
                  "type": "SYK_HELGEDAG"
                },
                {
                  "dagen": "2020-01-06",
                  "type": "SYK_HELGEDAG"
                }
              ],
              "beregnetSykdomstidslinje": [
                {
                  "dagen": "2020-01-01",
                  "type": "SYKEDAG_SYKMELDING"
                },
                {
                  "dagen": "2020-01-02",
                  "type": "SYK_HELGEDAG"
                },
                {
                  "dagen": "2020-01-03",
                  "type": "SYK_HELGEDAG"
                },
                {
                  "dagen": "2020-01-04",
                  "type": "SYKEDAG_SYKMELDING"
                },
                {
                  "dagen": "2020-01-05",
                  "type": "SYK_HELGEDAG"
                },
                {
                  "dagen": "2020-01-06",
                  "type": "SYK_HELGEDAG"
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
