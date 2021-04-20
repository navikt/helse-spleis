package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

internal class V90HendelsekildeTidsstempelTest {
    private val meldingerSupplier = MeldingerSupplier {
        mapOf(
            UUID.fromString("e878d0ab-a739-412c-a990-a2f2d75171f3") to """{ "sykmeldingSkrevet": "2021-03-20T12:00:00.100022738" }""", // Sykmelding
            UUID.fromString("debff1f8-f721-4506-8f80-7e89078cb85a") to """{ "sykmeldingSkrevet": "2021-03-10T12:00:00.100022738" }""", // Sykmelding
            UUID.fromString("66c0e3cf-8773-4c5c-bc3c-54d94cdf323f") to """{ "sykmeldingSkrevet": "2021-03-15T12:00:00.100022738" }""", // Søknad
            UUID.fromString("282bedc2-3cbc-4c34-9e59-1dc04ec33da9") to """{ "sykmeldingSkrevet": "2021-03-05T12:00:00.100022738" }""", // Søknad
            UUID.fromString("df41b72d-0fa5-4e3f-b39d-360585d88f4c") to """{ "mottattDato": "2021-03-01T12:00:00.100022738" }""", // Inntektsmelding
            UUID.fromString("376c35a1-8be7-4a69-8eda-8a4881bbcf84") to """{ "@opprettet": "2021-02-20T12:00:00.100022738" }""" // Inntektsmelding
        )
    }

    // 2b37f20d-e826-4b9e-9bb1-bd5713799483 overstyr
    // d68a7798-d732-44b3-9456-3301e5832014 ukjent

    @Test
    fun `setter tidsstempel på kilde`() {
        assertEquals(toNode(expectedPerson), migrer(person))
    }

    private fun toNode(json: String) = serdeObjectMapper.readTree(json)

    private fun migrer(json: String) = listOf(V90HendelsekildeTidsstempel())
        .migrate(toNode(json), meldingerSupplier)

    @Language("JSON")
    private val person   = """
{
  "arbeidsgivere": [
    {
      "sykdomshistorikk": [
        {
          "tidsstempel": "2021-04-20T12:00:00.100022738",
          "hendelseId": "e878d0ab-a739-412c-a990-a2f2d75171f3",
          "hendelseSykdomstidslinje": {
            "dager": [
              {
                "kilde": {
                  "type": "Sykmelding",
                  "id": "e878d0ab-a739-412c-a990-a2f2d75171f3"
                }
              }
            ]
          },
          "beregnetSykdomstidslinje": {
            "dager": [
              {
                "kilde": {
                  "type": "Sykmelding",
                  "id": "debff1f8-f721-4506-8f80-7e89078cb85a"
                }
              },
              {
                "kilde": {
                  "type": "Søknad",
                  "id": "282bedc2-3cbc-4c34-9e59-1dc04ec33da9"
                }
              },
              {
                "kilde": {
                  "type": "Inntektsmelding",
                  "id": "376c35a1-8be7-4a69-8eda-8a4881bbcf84"
                }
              },
              {
                "kilde": {
                  "type": "OverstyrTidslinje",
                  "id": "2b37f20d-e826-4b9e-9bb1-bd5713799483"
                }
              },
              {
                "kilde": {
                  "type": "SykdomstidslinjeHendelse",
                  "id": "d68a7798-d732-44b3-9456-3301e5832014"
                }
              }
            ]
          }
        },
        {
          "tidsstempel": "2021-04-19T12:00:00.100022738",
          "hendelseId": "66c0e3cf-8773-4c5c-bc3c-54d94cdf323f",
          "hendelseSykdomstidslinje": {
            "dager": [
              {
                "kilde": {
                  "type": "Søknad",
                  "id": "66c0e3cf-8773-4c5c-bc3c-54d94cdf323f"
                }
              }
            ]
          },
          "beregnetSykdomstidslinje": {
            "dager": [
              {
                "kilde": {
                  "type": "Søknad",
                  "id": "282bedc2-3cbc-4c34-9e59-1dc04ec33da9"
                }
              },
              {
                "kilde": {
                  "type": "Inntektsmelding",
                  "id": "376c35a1-8be7-4a69-8eda-8a4881bbcf84"
                }
              },
              {
                "kilde": {
                  "type": "OverstyrTidslinje",
                  "id": "2b37f20d-e826-4b9e-9bb1-bd5713799483"
                }
              },
              {
                "kilde": {
                  "type": "SykdomstidslinjeHendelse",
                  "id": "d68a7798-d732-44b3-9456-3301e5832014"
                }
              }
            ]
          }
        },
        {
          "tidsstempel": "2021-04-18T12:00:00.100022738",
          "hendelseId": "df41b72d-0fa5-4e3f-b39d-360585d88f4c",
          "hendelseSykdomstidslinje": {
            "dager": [
              {
                "kilde": {
                  "type": "Inntektsmelding",
                  "id": "df41b72d-0fa5-4e3f-b39d-360585d88f4c"
                }
              }
            ]
          },
          "beregnetSykdomstidslinje": {
            "dager": [
              {
                "kilde": {
                  "type": "Inntektsmelding",
                  "id": "376c35a1-8be7-4a69-8eda-8a4881bbcf84"
                }
              },
              {
                "kilde": {
                  "type": "OverstyrTidslinje",
                  "id": "2b37f20d-e826-4b9e-9bb1-bd5713799483"
                }
              },
              {
                "kilde": {
                  "type": "SykdomstidslinjeHendelse",
                  "id": "d68a7798-d732-44b3-9456-3301e5832014"
                }
              }
            ]
          }
        },
        {
          "tidsstempel": "2021-04-17T12:00:00.100022738",
          "hendelseId": "2b37f20d-e826-4b9e-9bb1-bd5713799483",
          "hendelseSykdomstidslinje": {
            "dager": [
              {
                "kilde": {
                  "type": "OverstyrTidslinje",
                  "id": "2b37f20d-e826-4b9e-9bb1-bd5713799483"
                }
              },
              {
                "kilde": {
                  "type": "SykdomstidslinjeHendelse",
                  "id": "d68a7798-d732-44b3-9456-3301e5832014"
                }
              }
            ]
          },
          "beregnetSykdomstidslinje": {
            "dager": [
              {
                "kilde": {
                  "type": "OverstyrTidslinje",
                  "id": "2b37f20d-e826-4b9e-9bb1-bd5713799483"
                }
              },
              {
                "kilde": {
                  "type": "SykdomstidslinjeHendelse",
                  "id": "d68a7798-d732-44b3-9456-3301e5832014"
                }
              }
            ]
          }
        },
        {
          "tidsstempel": "2021-04-16T12:00:00.100022738",
          "hendelseSykdomstidslinje": {
            "dager": [
              {
                "kilde": {
                  "type": "SykdomstidslinjeHendelse",
                  "id": "d68a7798-d732-44b3-9456-3301e5832014"
                }
              }
            ]
          },
          "beregnetSykdomstidslinje": {
            "dager": [
              {
                "kilde": {
                  "type": "SykdomstidslinjeHendelse",
                  "id": "d68a7798-d732-44b3-9456-3301e5832014"
                }
              }
            ]
          }
        }
      ],
      "vedtaksperioder": [
        {
          "sykdomstidslinje": {
            "dager": [
              {
                "kilde": {
                  "type": "Sykmelding",
                  "id": "debff1f8-f721-4506-8f80-7e89078cb85a"
                }
              },
              {
                "kilde": {
                  "type": "Søknad",
                  "id": "282bedc2-3cbc-4c34-9e59-1dc04ec33da9"
                }
              },
              {
                "kilde": {
                  "type": "Inntektsmelding",
                  "id": "376c35a1-8be7-4a69-8eda-8a4881bbcf84"
                }
              },
              {
                "kilde": {
                  "type": "OverstyrTidslinje",
                  "id": "2b37f20d-e826-4b9e-9bb1-bd5713799483"
                }
              },
              {
                "kilde": {
                  "type": "SykdomstidslinjeHendelse",
                  "id": "d68a7798-d732-44b3-9456-3301e5832014"
                }
              }
            ]
          }
        }
      ],
      "forkastede": [
        {
          "vedtaksperiode": {
              "sykdomstidslinje": {
                "dager": [
                  {
                    "kilde": {
                      "type": "Sykmelding",
                      "id": "debff1f8-f721-4506-8f80-7e89078cb85a"
                    }
                  },
                  {
                    "kilde": {
                      "type": "Søknad",
                      "id": "282bedc2-3cbc-4c34-9e59-1dc04ec33da9"
                    }
                  },
                  {
                    "kilde": {
                      "type": "Inntektsmelding",
                      "id": "376c35a1-8be7-4a69-8eda-8a4881bbcf84"
                    }
                  },
                  {
                    "kilde": {
                      "type": "OverstyrTidslinje",
                      "id": "2b37f20d-e826-4b9e-9bb1-bd5713799483"
                    }
                  },
                  {
                    "kilde": {
                      "type": "SykdomstidslinjeHendelse",
                      "id": "d68a7798-d732-44b3-9456-3301e5832014"
                    }
                  }
                ]
              }
          }
        }
      ]
    }
  ],
  "skjemaVersjon": 89
}
"""

    @Language("JSON")
    val expectedPerson = """
{
  "arbeidsgivere": [
    {
      "sykdomshistorikk": [
        {
          "tidsstempel": "2021-04-20T12:00:00.100022738",
          "hendelseId": "e878d0ab-a739-412c-a990-a2f2d75171f3",
          "hendelseSykdomstidslinje": {
            "dager": [
              {
                "kilde": {
                  "type": "Sykmelding",
                  "id": "e878d0ab-a739-412c-a990-a2f2d75171f3",
                  "tidsstempel": "2021-03-20T12:00:00.100022738"
                }
              }
            ]
          },
          "beregnetSykdomstidslinje": {
            "dager": [
              {
                "kilde": {
                  "type": "Sykmelding",
                  "id": "debff1f8-f721-4506-8f80-7e89078cb85a",
                  "tidsstempel": "2021-03-10T12:00:00.100022738"
                }
              },
              {
                "kilde": {
                  "type": "Søknad",
                  "id": "282bedc2-3cbc-4c34-9e59-1dc04ec33da9",
                  "tidsstempel": "2021-03-05T12:00:00.100022738"
                }
              },
              {
                "kilde": {
                  "type": "Inntektsmelding",
                  "id": "376c35a1-8be7-4a69-8eda-8a4881bbcf84",
                  "tidsstempel": "2021-02-20T12:00:00.100022738"
                }
              },
              {
                "kilde": {
                  "type": "OverstyrTidslinje",
                  "id": "2b37f20d-e826-4b9e-9bb1-bd5713799483",
                  "tidsstempel": "2021-04-17T12:00:00.100022738"
                }
              },
              {
                "kilde": {
                  "type": "SykdomstidslinjeHendelse",
                  "id": "d68a7798-d732-44b3-9456-3301e5832014",
                  "tidsstempel": "2021-04-20T12:00:00.100022738"
                }
              }
            ]
          }
        },
        {
          "tidsstempel": "2021-04-19T12:00:00.100022738",
          "hendelseId": "66c0e3cf-8773-4c5c-bc3c-54d94cdf323f",
          "hendelseSykdomstidslinje": {
            "dager": [
              {
                "kilde": {
                  "type": "Søknad",
                  "id": "66c0e3cf-8773-4c5c-bc3c-54d94cdf323f",
                  "tidsstempel": "2021-03-15T12:00:00.100022738"
                }
              }
            ]
          },
          "beregnetSykdomstidslinje": {
            "dager": [
              {
                "kilde": {
                  "type": "Søknad",
                  "id": "282bedc2-3cbc-4c34-9e59-1dc04ec33da9",
                  "tidsstempel": "2021-03-05T12:00:00.100022738"
                }
              },
              {
                "kilde": {
                  "type": "Inntektsmelding",
                  "id": "376c35a1-8be7-4a69-8eda-8a4881bbcf84",
                  "tidsstempel": "2021-02-20T12:00:00.100022738"
                }
              },
              {
                "kilde": {
                  "type": "OverstyrTidslinje",
                  "id": "2b37f20d-e826-4b9e-9bb1-bd5713799483",
                  "tidsstempel": "2021-04-17T12:00:00.100022738"
                }
              },
              {
                "kilde": {
                  "type": "SykdomstidslinjeHendelse",
                  "id": "d68a7798-d732-44b3-9456-3301e5832014",
                  "tidsstempel": "2021-04-20T12:00:00.100022738"
                }
              }
            ]
          }
        },
        {
          "tidsstempel": "2021-04-18T12:00:00.100022738",
          "hendelseId": "df41b72d-0fa5-4e3f-b39d-360585d88f4c",
          "hendelseSykdomstidslinje": {
            "dager": [
              {
                "kilde": {
                  "type": "Inntektsmelding",
                  "id": "df41b72d-0fa5-4e3f-b39d-360585d88f4c",
                  "tidsstempel": "2021-03-01T12:00:00.100022738"
                }
              }
            ]
          },
          "beregnetSykdomstidslinje": {
            "dager": [
              {
                "kilde": {
                  "type": "Inntektsmelding",
                  "id": "376c35a1-8be7-4a69-8eda-8a4881bbcf84",
                  "tidsstempel": "2021-02-20T12:00:00.100022738"
                }
              },
              {
                "kilde": {
                  "type": "OverstyrTidslinje",
                  "id": "2b37f20d-e826-4b9e-9bb1-bd5713799483",
                  "tidsstempel": "2021-04-17T12:00:00.100022738"
                }
              },
              {
                "kilde": {
                  "type": "SykdomstidslinjeHendelse",
                  "id": "d68a7798-d732-44b3-9456-3301e5832014",
                  "tidsstempel": "2021-04-20T12:00:00.100022738"
                }
              }
            ]
          }
        },
        {
          "tidsstempel": "2021-04-17T12:00:00.100022738",
          "hendelseId": "2b37f20d-e826-4b9e-9bb1-bd5713799483",
          "hendelseSykdomstidslinje": {
            "dager": [
              {
                "kilde": {
                  "type": "OverstyrTidslinje",
                  "id": "2b37f20d-e826-4b9e-9bb1-bd5713799483",
                  "tidsstempel": "2021-04-17T12:00:00.100022738"
                }
              },
              {
                "kilde": {
                  "type": "SykdomstidslinjeHendelse",
                  "id": "d68a7798-d732-44b3-9456-3301e5832014",
                  "tidsstempel": "2021-04-20T12:00:00.100022738"
                }
              }
            ]
          },
          "beregnetSykdomstidslinje": {
            "dager": [
              {
                "kilde": {
                  "type": "OverstyrTidslinje",
                  "id": "2b37f20d-e826-4b9e-9bb1-bd5713799483",
                  "tidsstempel": "2021-04-17T12:00:00.100022738"
                }
              },
              {
                "kilde": {
                  "type": "SykdomstidslinjeHendelse",
                  "id": "d68a7798-d732-44b3-9456-3301e5832014",
                  "tidsstempel": "2021-04-20T12:00:00.100022738"
                }
              }
            ]
          }
        },
        {
          "tidsstempel": "2021-04-16T12:00:00.100022738",
          "hendelseSykdomstidslinje": {
            "dager": [
              {
                "kilde": {
                  "type": "SykdomstidslinjeHendelse",
                  "id": "d68a7798-d732-44b3-9456-3301e5832014",
                  "tidsstempel": "2021-04-20T12:00:00.100022738"
                }
              }
            ]
          },
          "beregnetSykdomstidslinje": {
            "dager": [
              {
                "kilde": {
                  "type": "SykdomstidslinjeHendelse",
                  "id": "d68a7798-d732-44b3-9456-3301e5832014",
                  "tidsstempel": "2021-04-20T12:00:00.100022738"
                }
              }
            ]
          }
        }
      ],
      "vedtaksperioder": [
        {
          "sykdomstidslinje": {
            "dager": [
              {
                "kilde": {
                  "type": "Sykmelding",
                  "id": "debff1f8-f721-4506-8f80-7e89078cb85a",
                  "tidsstempel": "2021-03-10T12:00:00.100022738"
                }
              },
              {
                "kilde": {
                  "type": "Søknad",
                  "id": "282bedc2-3cbc-4c34-9e59-1dc04ec33da9",
                  "tidsstempel": "2021-03-05T12:00:00.100022738"
                }
              },
              {
                "kilde": {
                  "type": "Inntektsmelding",
                  "id": "376c35a1-8be7-4a69-8eda-8a4881bbcf84",
                  "tidsstempel": "2021-02-20T12:00:00.100022738"
                }
              },
              {
                "kilde": {
                  "type": "OverstyrTidslinje",
                  "id": "2b37f20d-e826-4b9e-9bb1-bd5713799483",
                  "tidsstempel": "2021-04-17T12:00:00.100022738"
                }
              },
              {
                "kilde": {
                  "type": "SykdomstidslinjeHendelse",
                  "id": "d68a7798-d732-44b3-9456-3301e5832014",
                  "tidsstempel": "2021-04-20T12:00:00.100022738"
                }
              }
            ]
          }
        }
      ],
      "forkastede": [
        {
          "vedtaksperiode": {
              "sykdomstidslinje": {
                "dager": [
                  {
                    "kilde": {
                      "type": "Sykmelding",
                      "id": "debff1f8-f721-4506-8f80-7e89078cb85a",
                      "tidsstempel": "2021-03-10T12:00:00.100022738"
                    }
                  },
                  {
                    "kilde": {
                      "type": "Søknad",
                      "id": "282bedc2-3cbc-4c34-9e59-1dc04ec33da9",
                      "tidsstempel": "2021-03-05T12:00:00.100022738"
                    }
                  },
                  {
                    "kilde": {
                      "type": "Inntektsmelding",
                      "id": "376c35a1-8be7-4a69-8eda-8a4881bbcf84",
                      "tidsstempel": "2021-02-20T12:00:00.100022738"
                    }
                  },
                  {
                    "kilde": {
                      "type": "OverstyrTidslinje",
                      "id": "2b37f20d-e826-4b9e-9bb1-bd5713799483",
                      "tidsstempel": "2021-04-17T12:00:00.100022738"
                    }
                  },
                  {
                    "kilde": {
                      "type": "SykdomstidslinjeHendelse",
                      "id": "d68a7798-d732-44b3-9456-3301e5832014",
                      "tidsstempel": "2021-04-20T12:00:00.100022738"
                    }
                  }
                ]
              }
          }
        }
      ]
    }
  ],
  "skjemaVersjon": 90
}
    """
}
