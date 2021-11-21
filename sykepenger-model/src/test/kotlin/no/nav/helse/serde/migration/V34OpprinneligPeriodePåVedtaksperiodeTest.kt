package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V34OpprinneligPeriodePåVedtaksperiodeTest {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `kopiere fom og tom fra sykmeldingstidslinje til sykmeldingsperiode`() {
        val migrated = listOf(V34OpprinneligPeriodePåVedtaksperiode())
            .migrate(objectMapper.readTree(originalJson()))
        val expected = objectMapper.readTree(expectedJson())

        assertEquals(expected, migrated)
    }

    @Test
    fun `med flere vedtaksperioder`() {
        val migrated = listOf(V34OpprinneligPeriodePåVedtaksperiode())
            .migrate(objectMapper.readTree(originalJsonMedFlereVedtaksperioder()))
        val expected = objectMapper.readTree(expectedJsonMedFlereVedtaksperioder())

        assertEquals(expected, migrated)
    }
}

@Language("JSON")
private fun originalJson() =
    """{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "sykdomshistorikk": [
            {
              "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03217",
              "tidsstempel": "2020-06-26T10:43:04.197284",
              "hendelseSykdomstidslinje": {},
              "beregnetSykdomstidslinje": {}
            },
            {
              "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03218",
              "tidsstempel": "2020-06-26T10:43:04.197284",
              "hendelseSykdomstidslinje": {},
              "beregnetSykdomstidslinje": {}
            },
            {
              "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03219",
              "tidsstempel": "2020-06-26T10:43:04.197284",
              "hendelseSykdomstidslinje": {
                "låstePerioder": [],
                "dager": [
                  {
                    "dato": "2020-01-01",
                    "type": "SYKEDAG",
                    "kilde": {
                      "type": "Sykmelding",
                      "id": "564ec4ef-8c2b-4089-964f-a5f65999ba4c"
                    }
                  },
                  {
                    "dato": "2020-01-02",
                    "type": "SYKEDAG",
                    "kilde": {
                      "type": "Sykmelding",
                      "id": "564ec4ef-8c2b-4089-964f-a5f65999ba4c"
                    }
                  },
                  {
                    "dato": "2020-01-03",
                    "type": "SYKEDAG",
                    "kilde": {
                      "type": "Sykmelding",
                      "id": "564ec4ef-8c2b-4089-964f-a5f65999ba4c"
                    }
                  }
                ]
              },
              "beregnetSykdomstidslinje": {}
            }
          ]
        }
      ],
      "forkastede": [
        {
          "sykdomshistorikk": [
            {
              "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03217",
              "tidsstempel": "2020-06-26T10:43:04.197284",
              "hendelseSykdomstidslinje": {},
              "beregnetSykdomstidslinje": {}
            },
            {
              "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03218",
              "tidsstempel": "2020-06-26T10:43:04.197284",
              "hendelseSykdomstidslinje": {},
              "beregnetSykdomstidslinje": {}
            },
            {
              "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03219",
              "tidsstempel": "2020-06-26T10:43:04.197284",
              "hendelseSykdomstidslinje": {
                "låstePerioder": [],
                "dager": [
                  {
                    "dato": "2020-02-01",
                    "type": "SYKEDAG",
                    "kilde": {
                      "type": "Sykmelding",
                      "id": "564ec4ef-8c2b-4089-964f-a5f65999ba4c"
                    }
                  },
                  {
                    "dato": "2020-02-02",
                    "type": "SYKEDAG",
                    "kilde": {
                      "type": "Sykmelding",
                      "id": "564ec4ef-8c2b-4089-964f-a5f65999ba4c"
                    }
                  },
                  {
                    "dato": "2020-02-03",
                    "type": "SYKEDAG",
                    "kilde": {
                      "type": "Sykmelding",
                      "id": "564ec4ef-8c2b-4089-964f-a5f65999ba4c"
                    }
                  }
                ]
              },
              "beregnetSykdomstidslinje": {}
            }
          ]
        }
      ]
    }
  ],
  "skjemaVersjon": 28
}
"""

@Language("JSON")
private fun expectedJson() =
    """{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "sykdomshistorikk": [
            {
              "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03217",
              "tidsstempel": "2020-06-26T10:43:04.197284",
              "hendelseSykdomstidslinje": {},
              "beregnetSykdomstidslinje": {}
            },
            {
              "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03218",
              "tidsstempel": "2020-06-26T10:43:04.197284",
              "hendelseSykdomstidslinje": {},
              "beregnetSykdomstidslinje": {}
            },
            {
              "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03219",
              "tidsstempel": "2020-06-26T10:43:04.197284",
              "hendelseSykdomstidslinje": {
                "låstePerioder": [],
                "dager": [
                  {
                    "dato": "2020-01-01",
                    "type": "SYKEDAG",
                    "kilde": {
                      "type": "Sykmelding",
                      "id": "564ec4ef-8c2b-4089-964f-a5f65999ba4c"
                    }
                  },
                  {
                    "dato": "2020-01-02",
                    "type": "SYKEDAG",
                    "kilde": {
                      "type": "Sykmelding",
                      "id": "564ec4ef-8c2b-4089-964f-a5f65999ba4c"
                    }
                  },
                  {
                    "dato": "2020-01-03",
                    "type": "SYKEDAG",
                    "kilde": {
                      "type": "Sykmelding",
                      "id": "564ec4ef-8c2b-4089-964f-a5f65999ba4c"
                    }
                  }
                ]
              },
              "beregnetSykdomstidslinje": {}
            }
          ],
          "sykmeldingFom": "2020-01-01",
          "sykmeldingTom": "2020-01-03"
        }
      ],
      "forkastede": [
        {
          "sykdomshistorikk": [
            {
              "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03217",
              "tidsstempel": "2020-06-26T10:43:04.197284",
              "hendelseSykdomstidslinje": {},
              "beregnetSykdomstidslinje": {}
            },
            {
              "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03218",
              "tidsstempel": "2020-06-26T10:43:04.197284",
              "hendelseSykdomstidslinje": {},
              "beregnetSykdomstidslinje": {}
            },
            {
              "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03219",
              "tidsstempel": "2020-06-26T10:43:04.197284",
              "hendelseSykdomstidslinje": {
                "låstePerioder": [],
                "dager": [
                  {
                    "dato": "2020-02-01",
                    "type": "SYKEDAG",
                    "kilde": {
                      "type": "Sykmelding",
                      "id": "564ec4ef-8c2b-4089-964f-a5f65999ba4c"
                    }
                  },
                  {
                    "dato": "2020-02-02",
                    "type": "SYKEDAG",
                    "kilde": {
                      "type": "Sykmelding",
                      "id": "564ec4ef-8c2b-4089-964f-a5f65999ba4c"
                    }
                  },
                  {
                    "dato": "2020-02-03",
                    "type": "SYKEDAG",
                    "kilde": {
                      "type": "Sykmelding",
                      "id": "564ec4ef-8c2b-4089-964f-a5f65999ba4c"
                    }
                  }
                ]
              },
              "beregnetSykdomstidslinje": {}
            }
          ],
          "sykmeldingFom": "2020-02-01",
          "sykmeldingTom": "2020-02-03"
        }
      ]
    }
  ],
  "skjemaVersjon":34
}
"""

@Language("JSON")
private fun originalJsonMedFlereVedtaksperioder() =
    """{
    "arbeidsgivere": [
        {
            "vedtaksperioder": [
                {
                    "sykdomshistorikk": [
                        {
                            "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03217",
                            "tidsstempel": "2020-06-26T10:43:04.197284",
                            "hendelseSykdomstidslinje": {},
                            "beregnetSykdomstidslinje": {}
                        },
                        {
                            "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03218",
                            "tidsstempel": "2020-06-26T10:43:04.197284",
                            "hendelseSykdomstidslinje": {},
                            "beregnetSykdomstidslinje": {}
                        },
                        {
                            "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03219",
                            "tidsstempel": "2020-06-26T10:43:04.197284",
                            "hendelseSykdomstidslinje": {
                                "låstePerioder": [],
                                "dager": [
                                    {
                                        "dato": "2020-01-01",
                                        "type": "SYKEDAG",
                                        "kilde": {
                                            "type": "Sykmelding",
                                            "id": "564ec4ef-8c2b-4089-964f-a5f65999ba4c"
                                        }
                                    },
                                    {
                                        "dato": "2020-01-02",
                                        "type": "SYKEDAG",
                                        "kilde": {
                                            "type": "Sykmelding",
                                            "id": "564ec4ef-8c2b-4089-964f-a5f65999ba4c"
                                        }
                                    },
                                    {
                                        "dato": "2020-01-03",
                                        "type": "SYKEDAG",
                                        "kilde": {
                                            "type": "Sykmelding",
                                            "id": "564ec4ef-8c2b-4089-964f-a5f65999ba4c"
                                        }
                                    }
                                ]
                            },
                            "beregnetSykdomstidslinje": {}
                        }
                    ]
                },
                {
                    "sykdomshistorikk": [
                        {
                            "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03217",
                            "tidsstempel": "2020-06-26T10:43:04.197284",
                            "hendelseSykdomstidslinje": {},
                            "beregnetSykdomstidslinje": {}
                        },
                        {
                            "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03218",
                            "tidsstempel": "2020-06-26T10:43:04.197284",
                            "hendelseSykdomstidslinje": {},
                            "beregnetSykdomstidslinje": {}
                        },
                        {
                            "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03219",
                            "tidsstempel": "2020-06-26T10:43:04.197284",
                            "hendelseSykdomstidslinje": {
                                "låstePerioder": [],
                                "dager": [
                                    {
                                        "dato": "2020-03-01",
                                        "type": "SYKEDAG",
                                        "kilde": {
                                            "type": "Sykmelding",
                                            "id": "564ec4ef-8c2b-4089-964f-a5f65999ba4c"
                                        }
                                    },
                                    {
                                        "dato": "2020-03-02",
                                        "type": "SYKEDAG",
                                        "kilde": {
                                            "type": "Sykmelding",
                                            "id": "564ec4ef-8c2b-4089-964f-a5f65999ba4c"
                                        }
                                    },
                                    {
                                        "dato": "2020-03-03",
                                        "type": "SYKEDAG",
                                        "kilde": {
                                            "type": "Sykmelding",
                                            "id": "564ec4ef-8c2b-4089-964f-a5f65999ba4c"
                                        }
                                    }
                                ]
                            },
                            "beregnetSykdomstidslinje": {}
                        }
                    ]
                }
            ],
            "forkastede": [
                {
                    "sykdomshistorikk": [
                        {
                            "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03217",
                            "tidsstempel": "2020-06-26T10:43:04.197284",
                            "hendelseSykdomstidslinje": {},
                            "beregnetSykdomstidslinje": {}
                        },
                        {
                            "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03218",
                            "tidsstempel": "2020-06-26T10:43:04.197284",
                            "hendelseSykdomstidslinje": {},
                            "beregnetSykdomstidslinje": {}
                        },
                        {
                            "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03219",
                            "tidsstempel": "2020-06-26T10:43:04.197284",
                            "hendelseSykdomstidslinje": {
                                "låstePerioder": [],
                                "dager": [
                                    {
                                        "dato": "2020-04-01",
                                        "type": "SYKEDAG",
                                        "kilde": {
                                            "type": "Sykmelding",
                                            "id": "564ec4ef-8c2b-4089-964f-a5f65999ba4c"
                                        }
                                    },
                                    {
                                        "dato": "2020-04-02",
                                        "type": "SYKEDAG",
                                        "kilde": {
                                            "type": "Sykmelding",
                                            "id": "564ec4ef-8c2b-4089-964f-a5f65999ba4c"
                                        }
                                    },
                                    {
                                        "dato": "2020-04-03",
                                        "type": "SYKEDAG",
                                        "kilde": {
                                            "type": "Sykmelding",
                                            "id": "564ec4ef-8c2b-4089-964f-a5f65999ba4c"
                                        }
                                    }
                                ]
                            },
                            "beregnetSykdomstidslinje": {}
                        }
                    ]
                },
                {
                    "sykdomshistorikk": [
                        {
                            "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03217",
                            "tidsstempel": "2020-06-26T10:43:04.197284",
                            "hendelseSykdomstidslinje": {},
                            "beregnetSykdomstidslinje": {}
                        },
                        {
                            "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03218",
                            "tidsstempel": "2020-06-26T10:43:04.197284",
                            "hendelseSykdomstidslinje": {},
                            "beregnetSykdomstidslinje": {}
                        },
                        {
                            "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03219",
                            "tidsstempel": "2020-06-26T10:43:04.197284",
                            "hendelseSykdomstidslinje": {
                                "låstePerioder": [],
                                "dager": [
                                    {
                                        "dato": "2020-02-01",
                                        "type": "SYKEDAG",
                                        "kilde": {
                                            "type": "Sykmelding",
                                            "id": "564ec4ef-8c2b-4089-964f-a5f65999ba4c"
                                        }
                                    },
                                    {
                                        "dato": "2020-02-02",
                                        "type": "SYKEDAG",
                                        "kilde": {
                                            "type": "Sykmelding",
                                            "id": "564ec4ef-8c2b-4089-964f-a5f65999ba4c"
                                        }
                                    },
                                    {
                                        "dato": "2020-02-03",
                                        "type": "SYKEDAG",
                                        "kilde": {
                                            "type": "Sykmelding",
                                            "id": "564ec4ef-8c2b-4089-964f-a5f65999ba4c"
                                        }
                                    }
                                ]
                            },
                            "beregnetSykdomstidslinje": {}
                        }
                    ]
                }
            ]
        }
    ],
    "skjemaVersjon": 28
}
"""

@Language("JSON")
private fun expectedJsonMedFlereVedtaksperioder() =
    """{
    "arbeidsgivere": [
        {
            "vedtaksperioder": [
                {
                    "sykdomshistorikk": [
                        {
                            "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03217",
                            "tidsstempel": "2020-06-26T10:43:04.197284",
                            "hendelseSykdomstidslinje": {},
                            "beregnetSykdomstidslinje": {}
                        },
                        {
                            "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03218",
                            "tidsstempel": "2020-06-26T10:43:04.197284",
                            "hendelseSykdomstidslinje": {},
                            "beregnetSykdomstidslinje": {}
                        },
                        {
                            "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03219",
                            "tidsstempel": "2020-06-26T10:43:04.197284",
                            "hendelseSykdomstidslinje": {
                                "låstePerioder": [],
                                "dager": [
                                    {
                                        "dato": "2020-01-01",
                                        "type": "SYKEDAG",
                                        "kilde": {
                                            "type": "Sykmelding",
                                            "id": "564ec4ef-8c2b-4089-964f-a5f65999ba4c"
                                        }
                                    },
                                    {
                                        "dato": "2020-01-02",
                                        "type": "SYKEDAG",
                                        "kilde": {
                                            "type": "Sykmelding",
                                            "id": "564ec4ef-8c2b-4089-964f-a5f65999ba4c"
                                        }
                                    },
                                    {
                                        "dato": "2020-01-03",
                                        "type": "SYKEDAG",
                                        "kilde": {
                                            "type": "Sykmelding",
                                            "id": "564ec4ef-8c2b-4089-964f-a5f65999ba4c"
                                        }
                                    }
                                ]
                            },
                            "beregnetSykdomstidslinje": {}
                        }
                    ],
                    "sykmeldingFom": "2020-01-01",
                    "sykmeldingTom": "2020-01-03"
                },
                {
                    "sykdomshistorikk": [
                        {
                            "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03217",
                            "tidsstempel": "2020-06-26T10:43:04.197284",
                            "hendelseSykdomstidslinje": {},
                            "beregnetSykdomstidslinje": {}
                        },
                        {
                            "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03218",
                            "tidsstempel": "2020-06-26T10:43:04.197284",
                            "hendelseSykdomstidslinje": {},
                            "beregnetSykdomstidslinje": {}
                        },
                        {
                            "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03219",
                            "tidsstempel": "2020-06-26T10:43:04.197284",
                            "hendelseSykdomstidslinje": {
                                "låstePerioder": [],
                                "dager": [
                                    {
                                        "dato": "2020-03-01",
                                        "type": "SYKEDAG",
                                        "kilde": {
                                            "type": "Sykmelding",
                                            "id": "564ec4ef-8c2b-4089-964f-a5f65999ba4c"
                                        }
                                    },
                                    {
                                        "dato": "2020-03-02",
                                        "type": "SYKEDAG",
                                        "kilde": {
                                            "type": "Sykmelding",
                                            "id": "564ec4ef-8c2b-4089-964f-a5f65999ba4c"
                                        }
                                    },
                                    {
                                        "dato": "2020-03-03",
                                        "type": "SYKEDAG",
                                        "kilde": {
                                            "type": "Sykmelding",
                                            "id": "564ec4ef-8c2b-4089-964f-a5f65999ba4c"
                                        }
                                    }
                                ]
                            },
                            "beregnetSykdomstidslinje": {}
                        }
                    ],
                    "sykmeldingFom": "2020-03-01",
                    "sykmeldingTom": "2020-03-03"
                }
            ],
            "forkastede": [
                {
                    "sykdomshistorikk": [
                        {
                            "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03217",
                            "tidsstempel": "2020-06-26T10:43:04.197284",
                            "hendelseSykdomstidslinje": {},
                            "beregnetSykdomstidslinje": {}
                        },
                        {
                            "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03218",
                            "tidsstempel": "2020-06-26T10:43:04.197284",
                            "hendelseSykdomstidslinje": {},
                            "beregnetSykdomstidslinje": {}
                        },
                        {
                            "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03219",
                            "tidsstempel": "2020-06-26T10:43:04.197284",
                            "hendelseSykdomstidslinje": {
                                "låstePerioder": [],
                                "dager": [
                                    {
                                        "dato": "2020-04-01",
                                        "type": "SYKEDAG",
                                        "kilde": {
                                            "type": "Sykmelding",
                                            "id": "564ec4ef-8c2b-4089-964f-a5f65999ba4c"
                                        }
                                    },
                                    {
                                        "dato": "2020-04-02",
                                        "type": "SYKEDAG",
                                        "kilde": {
                                            "type": "Sykmelding",
                                            "id": "564ec4ef-8c2b-4089-964f-a5f65999ba4c"
                                        }
                                    },
                                    {
                                        "dato": "2020-04-03",
                                        "type": "SYKEDAG",
                                        "kilde": {
                                            "type": "Sykmelding",
                                            "id": "564ec4ef-8c2b-4089-964f-a5f65999ba4c"
                                        }
                                    }
                                ]
                            },
                            "beregnetSykdomstidslinje": {}
                        }
                    ],
                    "sykmeldingFom": "2020-04-01",
                    "sykmeldingTom": "2020-04-03"
                },
                {
                    "sykdomshistorikk": [
                        {
                            "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03217",
                            "tidsstempel": "2020-06-26T10:43:04.197284",
                            "hendelseSykdomstidslinje": {},
                            "beregnetSykdomstidslinje": {}
                        },
                        {
                            "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03218",
                            "tidsstempel": "2020-06-26T10:43:04.197284",
                            "hendelseSykdomstidslinje": {},
                            "beregnetSykdomstidslinje": {}
                        },
                        {
                            "hendelseId": "c40b316d-f5e1-4b66-9afc-b0ef0ef03219",
                            "tidsstempel": "2020-06-26T10:43:04.197284",
                            "hendelseSykdomstidslinje": {
                                "låstePerioder": [],
                                "dager": [
                                    {
                                        "dato": "2020-02-01",
                                        "type": "SYKEDAG",
                                        "kilde": {
                                            "type": "Sykmelding",
                                            "id": "564ec4ef-8c2b-4089-964f-a5f65999ba4c"
                                        }
                                    },
                                    {
                                        "dato": "2020-02-02",
                                        "type": "SYKEDAG",
                                        "kilde": {
                                            "type": "Sykmelding",
                                            "id": "564ec4ef-8c2b-4089-964f-a5f65999ba4c"
                                        }
                                    },
                                    {
                                        "dato": "2020-02-03",
                                        "type": "SYKEDAG",
                                        "kilde": {
                                            "type": "Sykmelding",
                                            "id": "564ec4ef-8c2b-4089-964f-a5f65999ba4c"
                                        }
                                    }
                                ]
                            },
                            "beregnetSykdomstidslinje": {}
                        }
                    ],
                    "sykmeldingFom": "2020-02-01",
                    "sykmeldingTom": "2020-02-03"
                }
            ]
        }
    ],
    "skjemaVersjon": 34
}
"""

