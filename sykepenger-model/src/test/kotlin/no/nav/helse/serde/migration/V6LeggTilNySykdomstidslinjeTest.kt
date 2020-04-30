package no.nav.helse.serde.migration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class V6LeggTilNySykdomstidslinjeTest {
    private val objectMapper = jacksonObjectMapper()
    private val hendelsetidslinjeKey = "hendelseSykdomstidslinje"
    private val beregnetTidslinjeKey = "beregnetSykdomstidslinje"
    private val nyHendelsetidslinjeKey = "nyHendelseSykdomstidslinje"
    private val nyBeregnetTidslinjeKey = "nyBeregnetSykdomstidslinje"

    @Test
    fun `legger til nye sykdomstidslinjer`() {
        val json = objectMapper.readTree(personJson)
        listOf(V6LeggTilNySykdomstidslinje()).migrate(json)
        val migratedJson = objectMapper.readTree(json.toString())

        migratedJson.path("arbeidsgivere")
            .first()
            .path("vedtaksperioder")
            .forEach { periode ->
                periode.path("sykdomshistorikk")
                    .forEach { element ->
                        assertNotNull(element[nyBeregnetTidslinjeKey])
                        assertNotNull(element[nyHendelsetidslinjeKey])
                    }
            }
    }

    @Test
    fun `nye sykdomstidslinjer får samme lengde som gamle`() {
        val json = objectMapper.readTree(personJson)
        listOf(V6LeggTilNySykdomstidslinje()).migrate(json)
        val migratedJson = objectMapper.readTree(json.toString())

        migratedJson.path("arbeidsgivere")
            .first()
            .path("vedtaksperioder")[1]
            .path("sykdomshistorikk")
            .first().apply {
                assertEquals(get(hendelsetidslinjeKey).size(), get(nyHendelsetidslinjeKey)["dager"].size())
                assertEquals(get(beregnetTidslinjeKey).size(), get(nyBeregnetTidslinjeKey)["dager"].size())
            }
    }

    @Test
    fun `nye problemdager får melding`() {
        val json = objectMapper.readTree(personJson)
        listOf(V6LeggTilNySykdomstidslinje()).migrate(json)
        val migratedJson = objectMapper.readTree(json.toString())

        val sykdomshistorikk = migratedJson.path("arbeidsgivere")
            .first()
            .path("vedtaksperioder")[1]
            .path("sykdomshistorikk")
            .first()
        assertNull(sykdomshistorikk[nyBeregnetTidslinjeKey]["dager"].first()["melding"])
        assertEquals("Konvertert dag", sykdomshistorikk[nyHendelsetidslinjeKey]["dager"].first()["melding"].asText())
    }

    @Test
    fun `nye dager får grad dersom gammel dag har grad`() {
        val json = objectMapper.readTree(personJson)
        listOf(V6LeggTilNySykdomstidslinje()).migrate(json)
        val migratedJson = objectMapper.readTree(json.toString())

        val sykdomshistorikk = migratedJson.path("arbeidsgivere")
            .first()
            .path("vedtaksperioder")[1]
            .path("sykdomshistorikk")[2]
        assertNull(sykdomshistorikk[nyBeregnetTidslinjeKey]["dager"].first()["grad"])
        assertNull(sykdomshistorikk[nyHendelsetidslinjeKey]["dager"].first()["grad"])
        assertEquals(80.0, sykdomshistorikk[nyBeregnetTidslinjeKey]["dager"].last()["grad"].asDouble())
        assertEquals(80.0, sykdomshistorikk[nyHendelsetidslinjeKey]["dager"].last()["grad"].asDouble())
    }

    @Test
    fun `nye dager får riktig kilde`() {
        val json = objectMapper.readTree(personJson)
        listOf(V6LeggTilNySykdomstidslinje()).migrate(json)

        val sykdomshistorikk = json.path("arbeidsgivere")
            .first()
            .path("vedtaksperioder")[1]
            .path("sykdomshistorikk")[1]
        assertEquals("Søknad", sykdomshistorikk[nyBeregnetTidslinjeKey]["dager"].first()["kilde"]["type"].asText())
        assertEquals("Sykmelding", sykdomshistorikk[nyBeregnetTidslinjeKey]["dager"][2]["kilde"]["type"].asText())
        assertEquals("Ingen", sykdomshistorikk[nyHendelsetidslinjeKey]["dager"][1]["kilde"]["type"].asText())
        assertEquals(
            "00000000-0000-0000-0000-000000000000",
            sykdomshistorikk[nyHendelsetidslinjeKey]["dager"][1]["kilde"]["id"].asText()
        )
    }

    @Test
    fun `nye dager får riktig kildeId`() {
        val json = objectMapper.readTree(personJson2)
        listOf(V6LeggTilNySykdomstidslinje()).migrate(json)
        val migratedJson = objectMapper.readTree(json.toString())

        val tidligsteSykdomshistorikk = migratedJson.path("arbeidsgivere")
            .first()
            .path("vedtaksperioder").first()
            .path("sykdomshistorikk").last()
        val senesteSykdomshistorikk = migratedJson.path("arbeidsgivere")
            .first()
            .path("vedtaksperioder").first()
            .path("sykdomshistorikk").first()

        assertEquals(tidligsteSykdomshistorikk["hendelseId"], senesteSykdomshistorikk[nyBeregnetTidslinjeKey]["dager"][1]["kilde"]["id"])
        assertEquals(senesteSykdomshistorikk["hendelseId"], senesteSykdomshistorikk[nyBeregnetTidslinjeKey]["dager"][0]["kilde"]["id"])
    }

    @Test
    fun `nye dager får riktig grad`() {
        val json = objectMapper.readTree(personJson)
        listOf(V6LeggTilNySykdomstidslinje()).migrate(json)
        val migratedJson = objectMapper.readTree(json.toString())

        val sykdomshistorikk = migratedJson.path("arbeidsgivere")
            .first()
            .path("vedtaksperioder").last()
            .path("sykdomshistorikk")[1]

        assertEquals(75.0, sykdomshistorikk[hendelsetidslinjeKey][3]["grad"].asDouble())
        assertEquals(sykdomshistorikk[hendelsetidslinjeKey][3]["grad"], sykdomshistorikk[nyHendelsetidslinjeKey]["dager"][3]["grad"])
    }

    @Test
    fun `dager fra forrige element behandles riktig`() {
        val json = objectMapper.readTree(personJson2)
        listOf(V6LeggTilNySykdomstidslinje()).migrate(json)
        val migratedJson = objectMapper.readTree(json.toString())

        val nyestehistorikk = migratedJson.path("arbeidsgivere")
            .first()
            .path("vedtaksperioder")[1]
            .path("sykdomshistorikk")[0]

        assertEquals("Inntektsmelding", nyestehistorikk[nyBeregnetTidslinjeKey]["dager"][0]["kilde"]["type"].asText())
    }
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
                    "hendelseId": "483c7972-87c9-4f18-8628-489b93da6d3f",
                    "hendelseSykdomstidslinje": [
                        {
                            "dagen": "2020-02-01",
                            "type": "ARBEIDSDAG_INNTEKTSMELDING"
                        }
                    ],
                    "beregnetSykdomstidslinje": [
                        {
                            "dagen": "2020-02-01",
                            "type": "ARBEIDSDAG_SØKNAD"
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
                  "type": "UBESTEMTDAG"
                },
                {
                  "dagen": "2020-01-02",
                  "type": "UBESTEMTDAG"
                },
                {
                  "dagen": "2020-01-03",
                  "type": "EGENMELDINGSDAG_INNTEKTSMELDING"
                },
                {
                  "dagen": "2020-01-04",
                  "type": "EGENMELDINGSDAG_SØKNAD"
                }
              ],
              "beregnetSykdomstidslinje": [
                {
                  "dagen": "2020-01-01",
                  "type": "FERIEDAG_INNTEKTSMELDING"
                },
                {
                  "dagen": "2020-01-02",
                  "type": "FERIEDAG_SØKNAD"
                },
                {
                  "dagen": "2020-01-03",
                  "type": "FRISK_HELGEDAG_INNTEKTSMELDING"
                },
                {
                  "dagen": "2020-01-04",
                  "type": "FRISK_HELGEDAG_SØKNAD"
                },
                {
                  "dagen": "2020-01-05",
                  "type": "IMPLISITT_DAG"
                },
                {
                  "dagen": "2020-01-06",
                  "type": "FORELDET_SYKEDAG"
                }
              ]
            },
            {
              "tidsstempel": "2020-02-07T15:43:10.346894",
              "hendelseId": "3bbc20fb-64d4-4aac-8e51-46ace187eae4",
              "hendelseSykdomstidslinje": [
                {
                  "dagen": "2020-01-01",
                  "type": "PERMISJONSDAG_SØKNAD"
                },
                {
                  "dagen": "2020-01-02",
                  "type": "PERMISJONSDAG_AAREG"
                },
                {
                  "dagen": "2020-01-03",
                  "type": "STUDIEDAG"
                },
                {
                  "dagen": "2020-01-04",
                  "type": "SYKEDAG_SYKMELDING",
                  "grad": 75.0
                }
              ],
              "beregnetSykdomstidslinje": [
                {
                  "dagen": "2020-01-01",
                  "type": "SYKEDAG_SØKNAD"
                },
                {
                  "dagen": "2020-01-02",
                  "type": "SYK_HELGEDAG_SØKNAD"
                },
                {
                  "dagen": "2020-01-03",
                  "type": "SYK_HELGEDAG_SYKMELDING"
                },
                {
                  "dagen": "2020-01-04",
                  "type": "UTENLANDSDAG"
                },
                {
                  "dagen": "2020-01-05",
                  "type": "SYK_HELGEDAG_SØKNAD"
                },
                {
                  "dagen": "2020-01-06",
                  "type": "SYK_HELGEDAG_SØKNAD"
                }
              ]
            },
            {
              "tidsstempel": "2020-02-07T15:43:10.345275",
              "hendelseId": "e03ef661-11ab-464a-a0af-05656ad753cd",
              "hendelseSykdomstidslinje": [
                {
                  "dagen": "2020-01-01",
                  "type": "ARBEIDSDAG_INNTEKTSMELDING"
                },
                {
                  "dagen": "2020-01-02",
                  "grad": 80.0,
                  "type": "SYK_HELGEDAG_SØKNAD"
                },
                {
                  "dagen": "2020-01-03",
                  "grad": 80.0,
                  "type": "SYK_HELGEDAG_SØKNAD"
                },
                {
                  "dagen": "2020-01-04",
                  "grad": 80.0,
                  "type": "SYKEDAG_SYKMELDING"
                },
                {
                  "dagen": "2020-01-05",
                  "grad": 80.0,
                  "type": "SYK_HELGEDAG_SØKNAD"
                },
                {
                  "dagen": "2020-01-06",
                  "grad": 80.0,
                  "type": "SYK_HELGEDAG_SØKNAD"
                }
              ],
              "beregnetSykdomstidslinje": [
                {
                  "dagen": "2020-01-01",
                  "type": "ARBEIDSDAG_INNTEKTSMELDING"
                },
                {
                  "dagen": "2020-01-02",
                  "grad": 80.0,
                  "type": "SYK_HELGEDAG_SØKNAD"
                },
                {
                  "dagen": "2020-01-03",
                  "grad": 80.0,
                  "type": "SYK_HELGEDAG_SØKNAD"
                },
                {
                  "dagen": "2020-01-04",
                  "grad": 80.0,
                  "type": "SYKEDAG_SYKMELDING"
                },
                {
                  "dagen": "2020-01-05",
                  "grad": 80.0,
                  "type": "SYK_HELGEDAG_SØKNAD"
                },
                {
                  "dagen": "2020-01-06",
                  "grad": 80.0,
                  "type": "SYK_HELGEDAG_SØKNAD"
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

@Language("JSON")
private const val personJson2 = """
{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "sykdomshistorikk": [
            {
              "tidsstempel": "2020-02-08T15:43:10.346894",
              "hendelseId": "3bbc20fb-64d4-4aac-8e51-46ace187eae4",
              "hendelseSykdomstidslinje": [
                {
                  "dagen": "2020-01-01",
                  "type": "SYKEDAG_SØKNAD"
                },
                {
                  "dagen": "2020-01-02",
                  "type": "EGENMELDINGSDAG_SØKNAD"
                },
                {
                  "dagen": "2020-01-03",
                  "type": "SYKEDAG_SØKNAD"
                },
                {
                  "dagen": "2020-01-04",
                  "type": "SYK_HELGEDAG_SØKNAD"
                }
              ],
              "beregnetSykdomstidslinje": [
                {
                  "dagen": "2020-01-01",
                  "type": "SYKEDAG_SØKNAD"
                },
                {
                  "dagen": "2020-01-02",
                  "type": "SYKEDAG_SYKMELDING"
                },
                {
                  "dagen": "2020-01-03",
                  "type": "SYKEDAG_SØKNAD"
                },
                {
                  "dagen": "2020-01-04",
                  "type": "SYK_HELGEDAG_SØKNAD"
                }
              ]
            },
            {
              "tidsstempel": "2020-02-07T15:43:10.350016",
              "hendelseId": "583c7972-87c9-4f18-8628-489b93da6d3e",
              "hendelseSykdomstidslinje": [
                {
                  "dagen": "2020-01-01",
                  "type": "SYKEDAG_SYKMELDING"
                },
                {
                  "dagen": "2020-01-02",
                  "type": "SYKEDAG_SYKMELDING"
                },
                {
                  "dagen": "2020-01-03",
                  "type": "SYKEDAG_SYKMELDING"
                },
                {
                  "dagen": "2020-01-04",
                  "type": "SYK_HELGEDAG_SYKMELDING"
                }
              ],
              "beregnetSykdomstidslinje": [
                {
                  "dagen": "2020-01-01",
                  "type": "SYKEDAG_SYKMELDING"
                },
                {
                  "dagen": "2020-01-02",
                  "type": "SYKEDAG_SYKMELDING"
                },
                {
                  "dagen": "2020-01-03",
                  "type": "SYKEDAG_SYKMELDING"
                },
                {
                  "dagen": "2020-01-04",
                  "type": "SYK_HELGEDAG_SYKMELDING"
                }
              ]
            }
          ]
        },
        {
          "sykdomshistorikk": [
            {
              "tidsstempel": "2020-02-08T15:43:10.346894",
              "hendelseId": "3bbc20fb-64d4-4aac-8e51-46ace187eae4",
              "hendelseSykdomstidslinje": [
                {
                  "dagen": "2020-01-01",
                  "type": "SYKEDAG_SØKNAD"
                }
              ],
              "beregnetSykdomstidslinje": [
                {
                  "dagen": "2020-01-01",
                  "type": "ARBEIDSDAG_INNTEKTSMELDING"
                }
              ]
            },
            {
              "tidsstempel": "2020-02-02T15:43:10.346894",
              "hendelseId": "46ace187-64d4-4aac-8e51-3bbc20fbeae4",
              "hendelseSykdomstidslinje": [
                {
                  "dagen": "2020-01-01",
                  "type": "ARBEIDSDAG_INNTEKTSMELDING"
                }
              ],
              "beregnetSykdomstidslinje": [
                {
                  "dagen": "2020-01-01",
                  "type": "ARBEIDSDAG_INNTEKTSMELDING"
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
