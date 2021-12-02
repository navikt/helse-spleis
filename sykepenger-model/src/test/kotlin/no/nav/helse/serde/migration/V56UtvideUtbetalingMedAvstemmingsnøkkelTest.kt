package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V56UtvideUtbetalingMedAvstemmingsnøkkelTest {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `lager avstemmingsnøkkel på utbetalinger`() {
        val migrated = listOf(V56UtvideUtbetalingMedAvstemmingsnøkkel())
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
        "melding": "Utbetalingen ble overført til Oppdrag/UR 2020-11-21T17:39:57.014, og har fått avstemmingsnøkkel 123456789",
        "detaljer": {},
        "tidsstempel": "2020-11-21 18:39:57.014"
      },
      {
        "kontekster": [
          2
        ],
        "alvorlighetsgrad": "INFO",
        "melding": "Utbetalingen ble overført til Oppdrag/UR 2020-01-01T17:39:57.014, og har fått avstemmingsnøkkel 987654321",
        "detaljer": {},
        "tidsstempel": "2020-11-21 18:39:57.014"
      }
    ],
    "kontekster": [
      {
        "kontekstType": "Sykmelding",
        "kontekstMap": {
          "aktørId": "12345",
          "fødselsnummer": "12029240045",
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
            "utbetalinger": [
                {
                  "id": "c11cdfbc-dcc4-4f29-9ef9-e0db7b197487"
                },
                {
                  "id": "9b842ebe-699c-4a57-9265-453ca1ace142"
                },
                {
                  "id": "db8c2c11-e6f6-4731-a184-b3f0393570fc"
                }
            ],
            "vedtaksperioder": [
              {
                "id": "2555046a-fad9-4c4f-9aba-de1721b2b497",
                "utbetalingId": "c11cdfbc-dcc4-4f29-9ef9-e0db7b197487"
              },
              {
                "id": "ef991e12-b97c-4cf9-9f34-542dba36c582"
              }
            ],
            "forkastede": [
              {
                "vedtaksperiode": {
                  "id": "7c253913-6b90-4435-af88-db2ee1909e6f",
                  "utbetalingId": "9b842ebe-699c-4a57-9265-453ca1ace142"
                }
              }
            ]
        }
    ],
    "skjemaVersjon": 55
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
        "melding": "Utbetalingen ble overført til Oppdrag/UR 2020-11-21T17:39:57.014, og har fått avstemmingsnøkkel 123456789",
        "detaljer": {},
        "tidsstempel": "2020-11-21 18:39:57.014"
      },
      {
        "kontekster": [
          2
        ],
        "alvorlighetsgrad": "INFO",
        "melding": "Utbetalingen ble overført til Oppdrag/UR 2020-01-01T17:39:57.014, og har fått avstemmingsnøkkel 987654321",
        "detaljer": {},
        "tidsstempel": "2020-11-21 18:39:57.014"
      }
    ],
    "kontekster": [
      {
        "kontekstType": "Sykmelding",
        "kontekstMap": {
          "aktørId": "12345",
          "fødselsnummer": "12029240045",
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
            "utbetalinger": [
                {
                  "id": "c11cdfbc-dcc4-4f29-9ef9-e0db7b197487",
                  "overføringstidspunkt": "2020-11-21T17:39:57.014",
                  "avstemmingsnøkkel": "123456789"
                },
                {
                  "id": "9b842ebe-699c-4a57-9265-453ca1ace142",
                  "overføringstidspunkt": "2020-01-01T17:39:57.014",
                  "avstemmingsnøkkel": "987654321"
                },
                {
                  "id": "db8c2c11-e6f6-4731-a184-b3f0393570fc"
                }
            ],
            "vedtaksperioder": [
              {
                "id": "2555046a-fad9-4c4f-9aba-de1721b2b497",
                "utbetalingId": "c11cdfbc-dcc4-4f29-9ef9-e0db7b197487"
              },
              {
                "id": "ef991e12-b97c-4cf9-9f34-542dba36c582"
              }
            ],
            "forkastede": [
              {
                "vedtaksperiode": {
                  "id": "7c253913-6b90-4435-af88-db2ee1909e6f",
                  "utbetalingId": "9b842ebe-699c-4a57-9265-453ca1ace142"
                }
              }
            ]
        }
    ],
    "skjemaVersjon": 56
}
"""
