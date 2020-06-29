package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class V27CachetSykdomstidslinjePåVedtaksperiodeTest {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `kopierer over sykdomstidslinje fra sykdomshistorikk til vedtaksperiode`() {
        val migrated = listOf(V27CachetSykdomstidslinjePåVedtaksperiode()).migrate(objectMapper.readTree(originalJson))
        val expected = objectMapper.readTree(expectedJson)

        assertEquals(expected, migrated)
    }
}

@Language("JSON")
private val originalJson = """
{
    "arbeidsgivere": [
        {
            "vedtaksperioder": [
                {
                    "sykdomshistorikk": [
                        {
                            "hendelseId": "9045b8c8-1838-45fe-8329-2db5d3073756",
                            "tidsstempel": "2020-06-29T11:06:51.531013",
                            "beregnetSykdomstidslinje": {
                                "låstePerioder": [],
                                "dager": [
                                    {
                                        "dato": "2018-02-01",
                                        "type": "SYKEDAG",
                                        "kilde": {
                                            "type": "Inntektsmelding",
                                            "id": "9045b8c8-1838-45fe-8329-2db5d3073756"
                                        },
                                        "grad": 100.0,
                                        "arbeidsgiverBetalingProsent": 100.0
                                    },
                                    {
                                        "dato": "2018-02-02",
                                        "type": "SYKEDAG",
                                        "kilde": {
                                            "type": "Søknad",
                                            "id": "87e55bf6-8730-4fca-991e-35c5ce27fb25"
                                        },
                                        "grad": 100.0,
                                        "arbeidsgiverBetalingProsent": 100.0
                                    },
                                    {
                                        "dato": "2018-02-28",
                                        "type": "SYKEDAG",
                                        "kilde": {
                                            "type": "Søknad",
                                            "id": "87e55bf6-8730-4fca-991e-35c5ce27fb25"
                                        },
                                        "grad": 100.0,
                                        "arbeidsgiverBetalingProsent": 100.0
                                    }
                                ]
                            }
                        },
                        {
                            "hendelseId": "87e55bf6-8730-4fca-991e-35c5ce27fb25",
                            "tidsstempel": "2020-06-29T11:06:51.481668",
                            "beregnetSykdomstidslinje": {
                                "låstePerioder": [],
                                "dager": [
                                    {
                                        "dato": "2018-02-01",
                                        "type": "SYKEDAG",
                                        "kilde": {
                                            "type": "Søknad",
                                            "id": "87e55bf6-8730-4fca-991e-35c5ce27fb25"
                                        },
                                        "grad": 100.0,
                                        "arbeidsgiverBetalingProsent": 100.0
                                    },
                                    {
                                        "dato": "2018-02-02",
                                        "type": "SYKEDAG",
                                        "kilde": {
                                            "type": "Søknad",
                                            "id": "87e55bf6-8730-4fca-991e-35c5ce27fb25"
                                        },
                                        "grad": 100.0,
                                        "arbeidsgiverBetalingProsent": 100.0
                                    },
                                    {
                                        "dato": "2018-02-28",
                                        "type": "SYKEDAG",
                                        "kilde": {
                                            "type": "Søknad",
                                            "id": "87e55bf6-8730-4fca-991e-35c5ce27fb25"
                                        },
                                        "grad": 100.0,
                                        "arbeidsgiverBetalingProsent": 100.0
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
                            "hendelseId": "7d1be71f-078e-43af-aedd-479a9ecc0dd0",
                            "tidsstempel": "2020-06-29T11:06:51.531013",
                            "beregnetSykdomstidslinje": {
                                "låstePerioder": [],
                                "dager": [
                                    {
                                        "dato": "2018-01-01",
                                        "type": "SYKEDAG",
                                        "kilde": {
                                            "type": "Søknad",
                                            "id": "9677526b-e722-4055-8e63-c538dcb75aeb"
                                        },
                                        "grad": 100.0,
                                        "arbeidsgiverBetalingProsent": 100.0
                                    },
                                    {
                                        "dato": "2018-01-02",
                                        "type": "SYKEDAG",
                                        "kilde": {
                                            "type": "Søknad",
                                            "id": "9677526b-e722-4055-8e63-c538dcb75aeb"
                                        },
                                        "grad": 100.0,
                                        "arbeidsgiverBetalingProsent": 100.0
                                    },
                                    {
                                        "dato": "2018-01-28",
                                        "type": "SYK_HELGEDAG",
                                        "kilde": {
                                            "type": "Søknad",
                                            "id": "9677526b-e722-4055-8e63-c538dcb75aeb"
                                        },
                                        "grad": 100.0,
                                        "arbeidsgiverBetalingProsent": 100.0
                                    }
                                ]
                            }
                        },
                        {
                            "hendelseId": "9677526b-e722-4055-8e63-c538dcb75aeb",
                            "tidsstempel": "2020-06-29T11:06:51.481668",
                            "beregnetSykdomstidslinje": {
                                "låstePerioder": [],
                                "dager": [
                                    {
                                        "dato": "2018-01-01",
                                        "type": "SYKEDAG",
                                        "kilde": {
                                            "type": "Søknad",
                                            "id": "9677526b-e722-4055-8e63-c538dcb75aeb"
                                        },
                                        "grad": 100.0,
                                        "arbeidsgiverBetalingProsent": 100.0
                                    },
                                    {
                                        "dato": "2018-01-02",
                                        "type": "SYKEDAG",
                                        "kilde": {
                                            "type": "Søknad",
                                            "id": "9677526b-e722-4055-8e63-c538dcb75aeb"
                                        },
                                        "grad": 100.0,
                                        "arbeidsgiverBetalingProsent": 100.0
                                    },
                                    {
                                        "dato": "2018-01-28",
                                        "type": "SYK_HELGEDAG",
                                        "kilde": {
                                            "type": "Søknad",
                                            "id": "9677526b-e722-4055-8e63-c538dcb75aeb"
                                        },
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
    "skjemaVersjon": 26
}
"""

@Language("JSON")
private val expectedJson = """
    {
      "arbeidsgivere": [
        {
          "vedtaksperioder": [
            {
              "sykdomstidslinje": {
                "låstePerioder": [],
                "dager": [
                  {
                    "dato": "2018-02-01",
                    "type": "SYKEDAG",
                    "kilde": {
                      "type": "Inntektsmelding",
                      "id": "9045b8c8-1838-45fe-8329-2db5d3073756"
                    },
                    "grad": 100.0,
                    "arbeidsgiverBetalingProsent": 100.0
                  },
                  {
                    "dato": "2018-02-02",
                    "type": "SYKEDAG",
                    "kilde": {
                      "type": "Søknad",
                      "id": "87e55bf6-8730-4fca-991e-35c5ce27fb25"
                    },
                    "grad": 100.0,
                    "arbeidsgiverBetalingProsent": 100.0
                  },
                  {
                    "dato": "2018-02-28",
                    "type": "SYKEDAG",
                    "kilde": {
                      "type": "Søknad",
                      "id": "87e55bf6-8730-4fca-991e-35c5ce27fb25"
                    },
                    "grad": 100.0,
                    "arbeidsgiverBetalingProsent": 100.0
                  }
                ]
              },
              "sykdomshistorikk": [
                {
                  "hendelseId": "9045b8c8-1838-45fe-8329-2db5d3073756",
                  "tidsstempel": "2020-06-29T11:06:51.531013",
                  "beregnetSykdomstidslinje": {
                    "låstePerioder": [],
                    "dager": [
                      {
                        "dato": "2018-02-01",
                        "type": "SYKEDAG",
                        "kilde": {
                          "type": "Inntektsmelding",
                          "id": "9045b8c8-1838-45fe-8329-2db5d3073756"
                        },
                        "grad": 100.0,
                        "arbeidsgiverBetalingProsent": 100.0
                      },
                      {
                        "dato": "2018-02-02",
                        "type": "SYKEDAG",
                        "kilde": {
                          "type": "Søknad",
                          "id": "87e55bf6-8730-4fca-991e-35c5ce27fb25"
                        },
                        "grad": 100.0,
                        "arbeidsgiverBetalingProsent": 100.0
                      },
                      {
                        "dato": "2018-02-28",
                        "type": "SYKEDAG",
                        "kilde": {
                          "type": "Søknad",
                          "id": "87e55bf6-8730-4fca-991e-35c5ce27fb25"
                        },
                        "grad": 100.0,
                        "arbeidsgiverBetalingProsent": 100.0
                      }
                    ]
                  }
                },
                {
                  "hendelseId": "87e55bf6-8730-4fca-991e-35c5ce27fb25",
                  "tidsstempel": "2020-06-29T11:06:51.481668",
                  "beregnetSykdomstidslinje": {
                    "låstePerioder": [],
                    "dager": [
                      {
                        "dato": "2018-02-01",
                        "type": "SYKEDAG",
                        "kilde": {
                          "type": "Søknad",
                          "id": "87e55bf6-8730-4fca-991e-35c5ce27fb25"
                        },
                        "grad": 100.0,
                        "arbeidsgiverBetalingProsent": 100.0
                      },
                      {
                        "dato": "2018-02-02",
                        "type": "SYKEDAG",
                        "kilde": {
                          "type": "Søknad",
                          "id": "87e55bf6-8730-4fca-991e-35c5ce27fb25"
                        },
                        "grad": 100.0,
                        "arbeidsgiverBetalingProsent": 100.0
                      },
                      {
                        "dato": "2018-02-28",
                        "type": "SYKEDAG",
                        "kilde": {
                          "type": "Søknad",
                          "id": "87e55bf6-8730-4fca-991e-35c5ce27fb25"
                        },
                        "grad": 100.0,
                        "arbeidsgiverBetalingProsent": 100.0
                      }
                    ]
                  }
                }
              ]
            }
          ],
          "forkastede": [
            {
              "sykdomstidslinje": {
                "låstePerioder": [],
                "dager": [
                  {
                    "dato": "2018-01-01",
                    "type": "SYKEDAG",
                    "kilde": {
                      "type": "Søknad",
                      "id": "9677526b-e722-4055-8e63-c538dcb75aeb"
                    },
                    "grad": 100.0,
                    "arbeidsgiverBetalingProsent": 100.0
                  },
                  {
                    "dato": "2018-01-02",
                    "type": "SYKEDAG",
                    "kilde": {
                      "type": "Søknad",
                      "id": "9677526b-e722-4055-8e63-c538dcb75aeb"
                    },
                    "grad": 100.0,
                    "arbeidsgiverBetalingProsent": 100.0
                  },
                  {
                    "dato": "2018-01-28",
                    "type": "SYK_HELGEDAG",
                    "kilde": {
                      "type": "Søknad",
                      "id": "9677526b-e722-4055-8e63-c538dcb75aeb"
                    },
                    "grad": 100.0,
                    "arbeidsgiverBetalingProsent": 100.0
                  }
                ]
              },
              "sykdomshistorikk": [
                {
                  "hendelseId": "7d1be71f-078e-43af-aedd-479a9ecc0dd0",
                  "tidsstempel": "2020-06-29T11:06:51.531013",
                  "beregnetSykdomstidslinje": {
                    "låstePerioder": [],
                    "dager": [
                      {
                        "dato": "2018-01-01",
                        "type": "SYKEDAG",
                        "kilde": {
                          "type": "Søknad",
                          "id": "9677526b-e722-4055-8e63-c538dcb75aeb"
                        },
                        "grad": 100.0,
                        "arbeidsgiverBetalingProsent": 100.0
                      },
                      {
                        "dato": "2018-01-02",
                        "type": "SYKEDAG",
                        "kilde": {
                          "type": "Søknad",
                          "id": "9677526b-e722-4055-8e63-c538dcb75aeb"
                        },
                        "grad": 100.0,
                        "arbeidsgiverBetalingProsent": 100.0
                      },
                      {
                        "dato": "2018-01-28",
                        "type": "SYK_HELGEDAG",
                        "kilde": {
                          "type": "Søknad",
                          "id": "9677526b-e722-4055-8e63-c538dcb75aeb"
                        },
                        "grad": 100.0,
                        "arbeidsgiverBetalingProsent": 100.0
                      }
                    ]
                  }
                },
                {
                  "hendelseId": "9677526b-e722-4055-8e63-c538dcb75aeb",
                  "tidsstempel": "2020-06-29T11:06:51.481668",
                  "beregnetSykdomstidslinje": {
                    "låstePerioder": [],
                    "dager": [
                      {
                        "dato": "2018-01-01",
                        "type": "SYKEDAG",
                        "kilde": {
                          "type": "Søknad",
                          "id": "9677526b-e722-4055-8e63-c538dcb75aeb"
                        },
                        "grad": 100.0,
                        "arbeidsgiverBetalingProsent": 100.0
                      },
                      {
                        "dato": "2018-01-02",
                        "type": "SYKEDAG",
                        "kilde": {
                          "type": "Søknad",
                          "id": "9677526b-e722-4055-8e63-c538dcb75aeb"
                        },
                        "grad": 100.0,
                        "arbeidsgiverBetalingProsent": 100.0
                      },
                      {
                        "dato": "2018-01-28",
                        "type": "SYK_HELGEDAG",
                        "kilde": {
                          "type": "Søknad",
                          "id": "9677526b-e722-4055-8e63-c538dcb75aeb"
                        },
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
      "skjemaVersjon": 27
    }
"""
