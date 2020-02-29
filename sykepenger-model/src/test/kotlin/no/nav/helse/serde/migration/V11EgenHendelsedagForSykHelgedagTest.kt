package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class V11EgenHendelsedagForSykHelgedagTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    internal fun `oversetter til nye dagtyper`() {
        val json = objectMapper.readTree(personJson)
        listOf(V11EgenHendelsedagForSykHelgedag()).migrate(json)
        val migratedJson = objectMapper.readTree(json.toString())

        val sykdomshistorikkFørstePeriode = migratedJson
            .path("arbeidsgivere")
            .first()
            .path("vedtaksperioder")
            .first()
            .path("sykdomshistorikk")

        assertEquals(1, sykdomshistorikkFørstePeriode.size())
        assertEquals("SYK_HELGEDAG_SYKMELDING", dager(sykdomshistorikkFørstePeriode.first().path("beregnetSykdomstidslinje"))["2020-02-01"])
        assertEquals("SYK_HELGEDAG_SYKMELDING", dager(sykdomshistorikkFørstePeriode.first().path("hendelseSykdomstidslinje"))["2020-02-01"])

        val sykdomshistorikkAndrePeriode = migratedJson
            .path("arbeidsgivere")
            .first()
            .path("vedtaksperioder")[1]
            .path("sykdomshistorikk")

        assertEquals(3, sykdomshistorikkAndrePeriode.size())

        val beregnetTidslinje = dager(sykdomshistorikkAndrePeriode.first().path("beregnetSykdomstidslinje"))

        assertEquals("SYK_HELGEDAG_SØKNAD", beregnetTidslinje["2020-01-02"])
        assertEquals("SYK_HELGEDAG_SØKNAD", beregnetTidslinje["2020-01-03"])
        assertEquals("SYK_HELGEDAG_SYKMELDING", beregnetTidslinje["2020-01-05"])
        assertEquals("SYK_HELGEDAG_SYKMELDING", beregnetTidslinje["2020-01-06"])

        val søknadHendelseTidslinje = dager(sykdomshistorikkAndrePeriode[1].path("hendelseSykdomstidslinje"))
        assertEquals("SYK_HELGEDAG_SØKNAD", søknadHendelseTidslinje["2020-01-02"])
        assertEquals("SYK_HELGEDAG_SØKNAD", søknadHendelseTidslinje["2020-01-03"])
        assertNull(søknadHendelseTidslinje["2020-01-05"])
        assertNull(søknadHendelseTidslinje["2020-01-06"])
        val søknadBeregnetTidslinje = dager(sykdomshistorikkAndrePeriode[1].path("beregnetSykdomstidslinje"))
        assertEquals("SYK_HELGEDAG_SØKNAD", søknadBeregnetTidslinje["2020-01-02"])
        assertEquals("SYK_HELGEDAG_SØKNAD", søknadBeregnetTidslinje["2020-01-03"])
        assertEquals("SYK_HELGEDAG_SYKMELDING", søknadBeregnetTidslinje["2020-01-05"])
        assertEquals("SYK_HELGEDAG_SYKMELDING", søknadBeregnetTidslinje["2020-01-06"])

        val sykmeldingHendelseTidslinje = dager(sykdomshistorikkAndrePeriode[2].path("hendelseSykdomstidslinje"))
        assertEquals("SYK_HELGEDAG_SYKMELDING", sykmeldingHendelseTidslinje["2020-01-02"])
        assertEquals("SYK_HELGEDAG_SYKMELDING", sykmeldingHendelseTidslinje["2020-01-03"])
        assertEquals("SYK_HELGEDAG_SYKMELDING", sykmeldingHendelseTidslinje["2020-01-05"])
        assertEquals("SYK_HELGEDAG_SYKMELDING", sykmeldingHendelseTidslinje["2020-01-06"])
        val sykmeldingBeregnetTidslinje = dager(sykdomshistorikkAndrePeriode[2].path("beregnetSykdomstidslinje"))
        assertEquals("SYK_HELGEDAG_SYKMELDING", sykmeldingBeregnetTidslinje["2020-01-02"])
        assertEquals("SYK_HELGEDAG_SYKMELDING", sykmeldingBeregnetTidslinje["2020-01-03"])
        assertEquals("SYK_HELGEDAG_SYKMELDING", sykmeldingBeregnetTidslinje["2020-01-05"])
        assertEquals("SYK_HELGEDAG_SYKMELDING", sykmeldingBeregnetTidslinje["2020-01-06"])
    }

    private fun dager(tidslinje: JsonNode) =
        tidslinje
            .map { it["dagen"].asText() to it["type"].asText() }
            .associateBy { it.first }
            .mapValues { it.value.second }
}

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
                            "type": "SYK_HELGEDAG"
                        }
                    ],
                    "beregnetSykdomstidslinje": [
                        {
                            "dagen": "2020-02-01",
                            "type": "SYK_HELGEDAG"
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
                  "type": "EGENMELDINGSDAG_INNTEKTSMELDING"
                },
                {
                  "dagen": "2020-01-02",
                  "type": "EGENMELDINGSDAG_INNTEKTSMELDING"
                },
                {
                  "dagen": "2020-01-03",
                  "type": "EGENMELDINGSDAG_INNTEKTSMELDING"
                },
                {
                  "dagen": "2020-01-04",
                  "type": "EGENMELDINGSDAG_INNTEKTSMELDING"
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
