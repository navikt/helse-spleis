package no.nav.helse.serde.migration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V22FjernFelterFraSykdomstidslinjeTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `Fjerner gruppeId`() {
        val json = objectMapper.readTree(personJson)
        listOf(V22FjernFelterFraSykdomstidslinje()).migrate(json)
        val migratedJson = objectMapper.readTree(json.toString())
        val expected = objectMapper.readTree(expectedPersonJson)
        assertEquals(expected, migratedJson)
    }
}

@Language("JSON")
private const val personJson = """
{
  "arbeidsgivere": [
    {
      "sykdomshistorikk": [
        {
          "hendelseSykdomstidslinje": {
            "id": "f5df2868-67f9-421c-a4fd-036f1f3e50a2",
            "tidsstempel": "2020-06-18T13:15:00.190058",
            "låstePerioder": []
          },
          "beregnetSykdomstidslinje": {
            "id": "44ba7a21-9877-4656-9467-b327bf3a6b31",
            "tidsstempel": "2020-06-18T13:15:00.206992",
            "låstePerioder": [
              {
                "fom": "2018-01-05",
                "tom": "2018-01-10"
              }
            ]
          }
        },
        {
          "hendelseSykdomstidslinje": {
            "id": "b129d7c4-22dc-4386-ac32-27178e723c73",
            "tidsstempel": "2020-06-18T13:15:00.087142",
            "låstePerioder": []
          },
          "beregnetSykdomstidslinje": {
            "id": "5d2b319d-c444-4c53-ad1d-b7d0af022c1d",
            "tidsstempel": "2020-06-18T13:15:00.167148",
            "låstePerioder": [
              {
                "fom": "2018-01-05",
                "tom": "2018-01-10"
              }
            ]
          }
        }
      ],
      "vedtaksperioder": [
        {
          "sykdomshistorikk": [
            {
              "hendelseSykdomstidslinje": {
                "id": "ab8ba7c0-a036-490b-b77c-3d0768c0f297",
                "tidsstempel": "2020-06-18T13:15:00.200915",
                "låstePerioder": []
              },
              "beregnetSykdomstidslinje": {
                "id": "26197ba3-c7b0-4d66-a084-b576c3fabcb9",
                "tidsstempel": "2020-06-18T13:15:00.20288",
                "låstePerioder": [
                  {
                    "fom": "2018-01-05",
                    "tom": "2018-01-10"
                  }
                ]
              }
            },
            {
              "hendelseSykdomstidslinje": {
                "id": "f2778b23-486c-4196-94cc-899f2445b755",
                "tidsstempel": "2020-06-18T13:15:00.126974",
                "låstePerioder": []
              },
              "beregnetSykdomstidslinje": {
                "id": "bcc439f5-fc01-4d36-a4a4-bf5a57f14751",
                "tidsstempel": "2020-06-18T13:15:00.152622",
                "låstePerioder": [
                  {
                    "fom": "2018-01-05",
                    "tom": "2018-01-10"
                  }
                ]
              }
            }
          ]
        },
        {
          "sykdomshistorikk": [
            {
              "hendelseSykdomstidslinje": {
                "id": "ab8ba7c0-a036-490b-b77c-3d0768c0f297",
                "tidsstempel": "2020-06-18T13:15:00.200915",
                "låstePerioder": []
              },
              "beregnetSykdomstidslinje": {
                "id": "26197ba3-c7b0-4d66-a084-b576c3fabcb9",
                "tidsstempel": "2020-06-18T13:15:00.20288",
                "låstePerioder": [
                  {
                    "fom": "2018-01-05",
                    "tom": "2018-01-10"
                  }
                ]
              }
            },
            {
              "hendelseSykdomstidslinje": {
                "id": "f2778b23-486c-4196-94cc-899f2445b755",
                "tidsstempel": "2020-06-18T13:15:00.126974",
                "låstePerioder": []
              },
              "beregnetSykdomstidslinje": {
                "id": "bcc439f5-fc01-4d36-a4a4-bf5a57f14751",
                "tidsstempel": "2020-06-18T13:15:00.152622",
                "låstePerioder": [
                  {
                    "fom": "2018-01-05",
                    "tom": "2018-01-10"
                  }
                ]
              }
            }
          ]
        }
      ],
      "forkastede": [
        {
        "sykdomshistorikk": [
          {
            "hendelseSykdomstidslinje": {
              "id": "ab8ba7c0-a036-490b-b77c-3d0768c0f297",
              "tidsstempel": "2020-06-18T13:15:00.200915",
              "låstePerioder": []
            },
            "beregnetSykdomstidslinje": {
              "id": "26197ba3-c7b0-4d66-a084-b576c3fabcb9",
              "tidsstempel": "2020-06-18T13:15:00.20288",
              "låstePerioder": [
                {
                  "fom": "2018-01-05",
                  "tom": "2018-01-10"
                }
              ]
            }
          },
          {
            "hendelseSykdomstidslinje": {
              "id": "f2778b23-486c-4196-94cc-899f2445b755",
              "tidsstempel": "2020-06-18T13:15:00.126974",
              "låstePerioder": []
            },
            "beregnetSykdomstidslinje": {
              "id": "bcc439f5-fc01-4d36-a4a4-bf5a57f14751",
              "tidsstempel": "2020-06-18T13:15:00.152622",
              "låstePerioder": [
                {
                  "fom": "2018-01-05",
                  "tom": "2018-01-10"
                }
              ]
            }
          }
        ]
      },
        {
        "sykdomshistorikk": [
          {
            "hendelseSykdomstidslinje": {
              "id": "ab8ba7c0-a036-490b-b77c-3d0768c0f297",
              "tidsstempel": "2020-06-18T13:15:00.200915",
              "låstePerioder": []
            },
            "beregnetSykdomstidslinje": {
              "id": "26197ba3-c7b0-4d66-a084-b576c3fabcb9",
              "tidsstempel": "2020-06-18T13:15:00.20288",
              "låstePerioder": [
                {
                  "fom": "2018-01-05",
                  "tom": "2018-01-10"
                }
              ]
            }
          },
          {
            "hendelseSykdomstidslinje": {
              "id": "f2778b23-486c-4196-94cc-899f2445b755",
              "tidsstempel": "2020-06-18T13:15:00.126974",
              "låstePerioder": []
            },
            "beregnetSykdomstidslinje": {
              "id": "bcc439f5-fc01-4d36-a4a4-bf5a57f14751",
              "tidsstempel": "2020-06-18T13:15:00.152622",
              "låstePerioder": [
                {
                  "fom": "2018-01-05",
                  "tom": "2018-01-10"
                }
              ]
            }
          }
        ]
      }
      ]
    }
  ]
}
"""

@Language("JSON")
private const val expectedPersonJson = """
{
  "arbeidsgivere": [
    {
      "sykdomshistorikk": [
        {
          "hendelseSykdomstidslinje": {
            "låstePerioder": []
          },
          "beregnetSykdomstidslinje": {
            "låstePerioder": [
              {
                "fom": "2018-01-05",
                "tom": "2018-01-10"
              }
            ]
          }
        },
        {
          "hendelseSykdomstidslinje": {
            "låstePerioder": []
          },
          "beregnetSykdomstidslinje": {
            "låstePerioder": [
              {
                "fom": "2018-01-05",
                "tom": "2018-01-10"
              }
            ]
          }
        }
      ],
      "vedtaksperioder": [
        {
          "sykdomshistorikk": [
            {
              "hendelseSykdomstidslinje": {
                "låstePerioder": []
              },
              "beregnetSykdomstidslinje": {
                "låstePerioder": [
                  {
                    "fom": "2018-01-05",
                    "tom": "2018-01-10"
                  }
                ]
              }
            },
            {
              "hendelseSykdomstidslinje": {
                "låstePerioder": []
              },
              "beregnetSykdomstidslinje": {
                "låstePerioder": [
                  {
                    "fom": "2018-01-05",
                    "tom": "2018-01-10"
                  }
                ]
              }
            }
          ]
        },
        {
          "sykdomshistorikk": [
            {
              "hendelseSykdomstidslinje": {
                "låstePerioder": []
              },
              "beregnetSykdomstidslinje": {
                "låstePerioder": [
                  {
                    "fom": "2018-01-05",
                    "tom": "2018-01-10"
                  }
                ]
              }
            },
            {
              "hendelseSykdomstidslinje": {
                "låstePerioder": []
              },
              "beregnetSykdomstidslinje": {
                "låstePerioder": [
                  {
                    "fom": "2018-01-05",
                    "tom": "2018-01-10"
                  }
                ]
              }
            }
          ]
        }
      ],
      "forkastede": [
        {
        "sykdomshistorikk": [
          {
            "hendelseSykdomstidslinje": {
              "låstePerioder": []
            },
            "beregnetSykdomstidslinje": {
              "låstePerioder": [
                {
                  "fom": "2018-01-05",
                  "tom": "2018-01-10"
                }
              ]
            }
          },
          {
            "hendelseSykdomstidslinje": {
              "låstePerioder": []
            },
            "beregnetSykdomstidslinje": {
              "låstePerioder": [
                {
                  "fom": "2018-01-05",
                  "tom": "2018-01-10"
                }
              ]
            }
          }
        ]
      },
        {
        "sykdomshistorikk": [
          {
            "hendelseSykdomstidslinje": {
              "låstePerioder": []
            },
            "beregnetSykdomstidslinje": {
              "låstePerioder": [
                {
                  "fom": "2018-01-05",
                  "tom": "2018-01-10"
                }
              ]
            }
          },
          {
            "hendelseSykdomstidslinje": {
              "låstePerioder": []
            },
            "beregnetSykdomstidslinje": {
              "låstePerioder": [
                {
                  "fom": "2018-01-05",
                  "tom": "2018-01-10"
                }
              ]
            }
          }
        ]
      }
      ]
    }
  ],
  "skjemaVersjon": 22
}
"""
