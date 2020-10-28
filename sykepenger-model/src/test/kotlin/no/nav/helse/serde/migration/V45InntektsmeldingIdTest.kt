package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class V45InntektsmeldingIdTest {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `setter inntektsmeldingId`() {
        val migrated = listOf(V45InntektsmeldingId())
            .migrate(objectMapper.readTree(originalJson()))
        val expected = objectMapper.readTree(expectedJson())

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
          "fom": "2020-09-01",
          "tom": "2020-09-02",
          "forlengelseFraInfotrygd": "JA"
        },
        {
          "fom": "2020-09-03",
          "tom": "2020-09-04",
          "forlengelseFraInfotrygd": "JA"
        },
        {
          "fom": "2020-09-10",
          "tom": "2020-09-11",
          "forlengelseFraInfotrygd": "IKKE_ETTERSPURT",
          "inntektsmeldingId": "14b77f27-a0a0-4a9c-9fa9-a55697175fd5"
        },
        {
          "fom": "2020-09-14",
          "tom": "2020-09-15",
          "forlengelseFraInfotrygd": "IKKE_ETTERSPURT"
        },
        {
          "fom": "2020-10-01",
          "tom": "2020-10-05",
          "forlengelseFraInfotrygd": "IKKE_ETTERSPURT",
          "hendelseIder": ["7829bb5d-caf8-479d-8ec9-47681b7aa71e", "f7fe0691-190c-4428-864b-830764b923e4", "3075d483-4b17-4343-90e1-f84153b25faa"],
          "sykdomshistorikk": [
            {
              "hendelseId": "7829bb5d-caf8-479d-8ec9-47681b7aa71e",
              "hendelseSykdomstidslinje": {
                "dager": [
                  {
                    "dato": "2020-10-01",
                    "kilde": {
                      "type": "Søknad"
                    }
                  }
                ]
              }
            },
            {
              "hendelseId": "f7fe0691-190c-4428-864b-830764b923e4",
              "hendelseSykdomstidslinje": {
                "dager": [
                  {
                    "dato": "2020-10-01",
                    "kilde": {
                      "type": "Inntektsmelding"
                    }
                  }
                ]
              }
            },
            {
              "hendelseId": "3075d483-4b17-4343-90e1-f84153b25faa",
              "hendelseSykdomstidslinje": {
                "dager": [
                  {
                    "dato": "2020-10-01",
                    "kilde": {
                      "type": "Sykmelding"
                    }
                  }
                ]
              }
            }
          ]
        },
        {
          "fom": "2020-10-06",
          "tom": "2020-10-10",
          "forlengelseFraInfotrygd": "IKKE_ETTERSPURT",
          "hendelseIder": ["a4abcac8-707e-4971-b58a-fe3af6111e99", "1f6f8a66-aa3f-461d-8269-f35839db5080"]
        },
        {
          "fom": "2020-10-20",
          "tom": "2020-10-30",
          "forlengelseFraInfotrygd": "IKKE_ETTERSPURT",
          "hendelseIder": ["7829bb5d-caf8-479d-8ec9-47681b7aa71e", "f7fe0691-190c-4428-864b-830764b923e4"]
        },
        {
          "fom": "2020-11-05",
          "tom": "2020-11-10",
          "forlengelseFraInfotrygd": "IKKE_ETTERSPURT",
          "hendelseIder": ["ccc78032-9f7f-482d-9094-f6e599cf6dbe", "a0c15710-9d23-48ef-8701-137dd5c8de72", "d015c06c-18f8-4fc6-bcb1-1de914047c15"],
          "sykdomshistorikk": [
            {
              "hendelseId": "ccc78032-9f7f-482d-9094-f6e599cf6dbe",
              "hendelseSykdomstidslinje": {
                "dager": [
                  {
                    "dato": "2020-11-05",
                    "kilde": {
                      "type": "Søknad"
                    }
                  }
                ]
              }
            },
            {
              "hendelseId": "a0c15710-9d23-48ef-8701-137dd5c8de72",
              "hendelseSykdomstidslinje": {
                "dager": [
                  {
                    "dato": "2020-11-05",
                    "kilde": {
                      "type": "Inntektsmelding"
                    }
                  }
                ]
              }
            },
            {
              "hendelseId": "d015c06c-18f8-4fc6-bcb1-1de914047c15",
              "hendelseSykdomstidslinje": {
                "dager": [
                  {
                    "dato": "2020-11-05",
                    "kilde": {
                      "type": "Sykmelding"
                    }
                  }
                ]
              }
            }
          ]
        },
        {
          "fom": "2020-11-12",
          "tom": "2020-11-15",
          "forlengelseFraInfotrygd": "IKKE_ETTERSPURT",
          "hendelseIder": ["7829bb5d-caf8-479d-8ec9-47681b7aa71e", "f7fe0691-190c-4428-864b-830764b923e4"]
        }
      ]
    }
  ],
  "skjemaVersjon": 44
}
"""

@Language("JSON")
private fun expectedJson() =
    """{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "fom": "2020-09-01",
          "tom": "2020-09-02",
          "forlengelseFraInfotrygd": "JA"
        },
        {
          "fom": "2020-09-03",
          "tom": "2020-09-04",
          "forlengelseFraInfotrygd": "JA"
        },
        {
          "fom": "2020-09-10",
          "tom": "2020-09-11",
          "forlengelseFraInfotrygd": "IKKE_ETTERSPURT",
          "inntektsmeldingId": "14b77f27-a0a0-4a9c-9fa9-a55697175fd5"
        },
        {
          "fom": "2020-09-14",
          "tom": "2020-09-15",
          "forlengelseFraInfotrygd": "IKKE_ETTERSPURT",
          "inntektsmeldingId": "14b77f27-a0a0-4a9c-9fa9-a55697175fd5",
          "hendelseIder": ["14b77f27-a0a0-4a9c-9fa9-a55697175fd5"]
        },
        {
          "fom": "2020-10-01",
          "tom": "2020-10-05",
          "forlengelseFraInfotrygd": "IKKE_ETTERSPURT",
          "inntektsmeldingId": "f7fe0691-190c-4428-864b-830764b923e4",
          "hendelseIder": ["7829bb5d-caf8-479d-8ec9-47681b7aa71e", "f7fe0691-190c-4428-864b-830764b923e4", "3075d483-4b17-4343-90e1-f84153b25faa"],
          "sykdomshistorikk": [
            {
              "hendelseId": "7829bb5d-caf8-479d-8ec9-47681b7aa71e",
              "hendelseSykdomstidslinje": {
                "dager": [
                  {
                    "dato": "2020-10-01",
                    "kilde": {
                      "type": "Søknad"
                    }
                  }
                ]
              }
            },
            {
              "hendelseId": "f7fe0691-190c-4428-864b-830764b923e4",
              "hendelseSykdomstidslinje": {
                "dager": [
                  {
                    "dato": "2020-10-01",
                    "kilde": {
                      "type": "Inntektsmelding"
                    }
                  }
                ]
              }
            },
            {
              "hendelseId": "3075d483-4b17-4343-90e1-f84153b25faa",
              "hendelseSykdomstidslinje": {
                "dager": [
                  {
                    "dato": "2020-10-01",
                    "kilde": {
                      "type": "Sykmelding"
                    }
                  }
                ]
              }
            }
          ]
        },
        {
          "fom": "2020-10-06",
          "tom": "2020-10-10",
          "forlengelseFraInfotrygd": "IKKE_ETTERSPURT",
          "inntektsmeldingId": "f7fe0691-190c-4428-864b-830764b923e4",
          "hendelseIder": ["a4abcac8-707e-4971-b58a-fe3af6111e99", "1f6f8a66-aa3f-461d-8269-f35839db5080", "f7fe0691-190c-4428-864b-830764b923e4"]
        },
        {
          "fom": "2020-10-20",
          "tom": "2020-10-30",
          "forlengelseFraInfotrygd": "IKKE_ETTERSPURT",
          "hendelseIder": ["7829bb5d-caf8-479d-8ec9-47681b7aa71e", "f7fe0691-190c-4428-864b-830764b923e4"]
        },
        {
          "fom": "2020-11-05",
          "tom": "2020-11-10",
          "forlengelseFraInfotrygd": "IKKE_ETTERSPURT",
          "hendelseIder": ["ccc78032-9f7f-482d-9094-f6e599cf6dbe", "a0c15710-9d23-48ef-8701-137dd5c8de72", "d015c06c-18f8-4fc6-bcb1-1de914047c15"],
          "inntektsmeldingId": "a0c15710-9d23-48ef-8701-137dd5c8de72",
          "sykdomshistorikk": [
            {
              "hendelseId": "ccc78032-9f7f-482d-9094-f6e599cf6dbe",
              "hendelseSykdomstidslinje": {
                "dager": [
                  {
                    "dato": "2020-11-05",
                    "kilde": {
                      "type": "Søknad"
                    }
                  }
                ]
              }
            },
            {
              "hendelseId": "a0c15710-9d23-48ef-8701-137dd5c8de72",
              "hendelseSykdomstidslinje": {
                "dager": [
                  {
                    "dato": "2020-11-05",
                    "kilde": {
                      "type": "Inntektsmelding"
                    }
                  }
                ]
              }
            },
            {
              "hendelseId": "d015c06c-18f8-4fc6-bcb1-1de914047c15",
              "hendelseSykdomstidslinje": {
                "dager": [
                  {
                    "dato": "2020-11-05",
                    "kilde": {
                      "type": "Sykmelding"
                    }
                  }
                ]
              }
            }
          ]
        },
        {
          "fom": "2020-11-12",
          "tom": "2020-11-15",
          "forlengelseFraInfotrygd": "IKKE_ETTERSPURT",
          "hendelseIder": ["7829bb5d-caf8-479d-8ec9-47681b7aa71e", "f7fe0691-190c-4428-864b-830764b923e4"]
        }
      ]
    }
  ],
  "skjemaVersjon": 45
}
"""
