package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V69SetteOpprettetOgOppdatertTidspunktTest {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `legger på opprettet- og oppdatert-tidspunkt`() {
        val migrated = listOf(V69SetteOpprettetOgOppdatertTidspunkt())
            .migrate(objectMapper.readTree(originalJson()))
        val expected = objectMapper.readTree(expectedJson())

        assertEquals(expected, migrated)
    }
}

@Language("JSON")
private fun originalJson() =
    """
{
  "aktivitetslogg": {
    "aktiviteter": [
      {
        "kontekster": [
          1
        ],
        "alvorlighetsgrad": "INFO",
        "melding": "Sykmelding ferdigbehandlet",
        "detaljer": {},
        "tidsstempel": "2020-11-20 12:00:00.100"
      },
      {
        "kontekster": [
          1
        ],
        "alvorlighetsgrad": "INFO",
        "melding": "Avventer godkjenning",
        "detaljer": {},
        "tidsstempel": "2020-11-30 09:00:00.200"
      },
      {
        "kontekster": [
          2
        ],
        "alvorlighetsgrad": "INFO",
        "melding": "Vedtaksperioden er utbetalt",
        "detaljer": {},
        "tidsstempel": "2020-12-12 15:10:10.100"
      }
    ],
    "kontekster": [
      {
        "kontekstType": "Sykmelding",
        "kontekstMap": {
          "aktørId": "12345",
          "fødselsnummer": "12020052345",
          "organisasjonsnummer": "987654321",
          "id": "f337c964-1cd6-404b-ab47-b4d453b78b9b"
        }
      },
      {
        "kontekstType": "Vedtaksperiode",
        "kontekstMap": {
          "vedtaksperiodeId": "2555046a-fad9-4c4f-9aba-de1721b2b497"
        }
      },
      {
        "kontekstType": "Vedtaksperiode",
        "kontekstMap": {
          "vedtaksperiodeId": "7c253913-6b90-4435-af88-db2ee1909e6f"
        }
      }
    ]
  },
    "arbeidsgivere": [
        {
            "vedtaksperioder": [
              {
                "id": "2555046a-fad9-4c4f-9aba-de1721b2b497"
              },
              {
                "id": "ef991e12-b97c-4cf9-9f34-542dba36c582"
              }
            ],
            "forkastede": [
              {
                "vedtaksperiode": {
                  "id": "7c253913-6b90-4435-af88-db2ee1909e6f"
                }
              }
            ]
        }
    ],
    "skjemaVersjon": 68
}
"""

@Language("JSON")
private fun expectedJson() =
    """
{
  "aktivitetslogg": {
    "aktiviteter": [
      {
        "kontekster": [
          1
        ],
        "alvorlighetsgrad": "INFO",
        "melding": "Sykmelding ferdigbehandlet",
        "detaljer": {},
        "tidsstempel": "2020-11-20 12:00:00.100"
      },
      {
        "kontekster": [
          1
        ],
        "alvorlighetsgrad": "INFO",
        "melding": "Avventer godkjenning",
        "detaljer": {},
        "tidsstempel": "2020-11-30 09:00:00.200"
      },
      {
        "kontekster": [
          2
        ],
        "alvorlighetsgrad": "INFO",
        "melding": "Vedtaksperioden er utbetalt",
        "detaljer": {},
        "tidsstempel": "2020-12-12 15:10:10.100"
      }
    ],
    "kontekster": [
      {
        "kontekstType": "Sykmelding",
        "kontekstMap": {
          "aktørId": "12345",
          "fødselsnummer": "12020052345",
          "organisasjonsnummer": "987654321",
          "id": "f337c964-1cd6-404b-ab47-b4d453b78b9b"
        }
      },
      {
        "kontekstType": "Vedtaksperiode",
        "kontekstMap": {
          "vedtaksperiodeId": "2555046a-fad9-4c4f-9aba-de1721b2b497"
        }
      },
      {
        "kontekstType": "Vedtaksperiode",
        "kontekstMap": {
          "vedtaksperiodeId": "7c253913-6b90-4435-af88-db2ee1909e6f"
        }
      }
    ]
  },
    "arbeidsgivere": [
        {
            "vedtaksperioder": [
              {
                "id": "2555046a-fad9-4c4f-9aba-de1721b2b497",
                "opprettet": "2020-11-20T12:00:00.100",
                "oppdatert": "2020-11-30T09:00:00.200"
              },
              {
                "id": "ef991e12-b97c-4cf9-9f34-542dba36c582",
                "opprettet": "-999999999-01-01T00:00",
                "oppdatert" : "-999999999-01-01T00:00"
              }
            ],
            "forkastede": [
              {
                "vedtaksperiode": {
                  "id": "7c253913-6b90-4435-af88-db2ee1909e6f",
                  "opprettet": "2020-12-12T15:10:10.100",
                  "oppdatert": "2020-12-12T15:10:10.100"
                }
              }
            ]
        }
    ],
    "skjemaVersjon": 69
}
"""
