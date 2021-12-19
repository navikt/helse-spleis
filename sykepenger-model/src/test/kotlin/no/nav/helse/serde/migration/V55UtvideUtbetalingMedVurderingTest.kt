package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V55UtvideUtbetalingMedVurderingTest {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `lager vurdering på utbetalinger`() {
        val migrated = listOf(V55UtvideUtbetalingMedVurdering())
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
          2
        ],
        "alvorlighetsgrad": "INFO",
        "melding": "Utbetaling markert som ikke godkjent",
        "detaljer": {},
        "tidsstempel": "2020-11-21 18:39:57.014"
      },
      {
        "kontekster": [
          1
        ],
        "alvorlighetsgrad": "INFO",
        "melding": "Noe skjedde",
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
                  "tidsstempel": "2020-04-21T18:39:57.114",
                  "annullert": false
                },
                {
                  "id": "9b842ebe-699c-4a57-9265-453ca1ace142",
                  "tidsstempel": "2020-05-20T20:00:01.500",
                  "annullert": false
                },
                {
                  "id": "be9faa2a-d11e-4d63-926c-bc217692206f",
                  "tidsstempel": "2020-06-20T20:00:01.500",
                  "annullert": false
                },
                {
                  "id": "59b68a98-838b-440d-8f90-f8bfae2bc922",
                  "tidsstempel": "2020-07-20T20:00:01.500",
                  "annullert": false
                },
                {
                  "id": "38599b67-0662-4e18-b3d9-c03d985dce34",
                  "tidsstempel": "2020-08-20T20:00:01.500",
                  "annullert": true
                }
            ],
            "vedtaksperioder": [
              {
                "id": "2555046a-fad9-4c4f-9aba-de1721b2b497",
                "utbetalingId": "9b842ebe-699c-4a57-9265-453ca1ace142",
                "godkjentAv": "S123456",
                "godkjenttidspunkt": "2020-01-01T00:00:00.500",
                "automatiskBehandling": null
              },
              {
                "id": "ef991e12-b97c-4cf9-9f34-542dba36c582",
                "utbetalingId": "be9faa2a-d11e-4d63-926c-bc217692206f",
                "godkjentAv": "K123456",
                "godkjenttidspunkt": "2020-10-01T00:00:00.500",
                "automatiskBehandling": true
              },
              {
                "id": "290c176c-0a3a-4651-84c7-0bf32b62d193",
                "utbetalingId": "59b68a98-838b-440d-8f90-f8bfae2bc922"
              }
            ],
            "forkastede": [
              {
                "vedtaksperiode": {
                  "id": "7c253913-6b90-4435-af88-db2ee1909e6f",
                  "utbetalingId": "c11cdfbc-dcc4-4f29-9ef9-e0db7b197487",
                  "tilstand": "TIL_INFOTRYGD"
                }
              }
            ]
        }
    ],
    "skjemaVersjon": 54
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
          2
        ],
        "alvorlighetsgrad": "INFO",
        "melding": "Utbetaling markert som ikke godkjent",
        "detaljer": {},
        "tidsstempel": "2020-11-21 18:39:57.014"
      },
      {
        "kontekster": [
          1
        ],
        "alvorlighetsgrad": "INFO",
        "melding": "Noe skjedde",
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
                  "tidsstempel": "2020-04-21T18:39:57.114",
                  "annullert": false,
                  "status": "IKKE_GODKJENT",
                  "vurdering": {
                      "ident": "Z999999",
                      "epost": "ukjent@nav.no",
                      "tidspunkt": "2020-11-21T18:39:57.014",
                      "automatiskBehandling": false
                  }
                },
                {
                  "id": "9b842ebe-699c-4a57-9265-453ca1ace142",
                  "tidsstempel": "2020-05-20T20:00:01.500",
                  "annullert": false,
                  "vurdering": {
                      "ident": "S123456",
                      "epost": "ukjent@nav.no",
                      "tidspunkt": "2020-01-01T00:00:00.500",
                      "automatiskBehandling": false
                  }
                },
                {
                  "id": "be9faa2a-d11e-4d63-926c-bc217692206f",
                  "tidsstempel": "2020-06-20T20:00:01.500",
                  "annullert": false,
                  "vurdering": {
                    "ident": "K123456",
                    "epost": "ukjent@nav.no",
                    "tidspunkt": "2020-10-01T00:00:00.500",
                    "automatiskBehandling": true
                  }
                },
                {
                  "id": "59b68a98-838b-440d-8f90-f8bfae2bc922",
                  "tidsstempel": "2020-07-20T20:00:01.500",
                  "annullert": false,
                  "vurdering": null
                },
                {
                  "id": "38599b67-0662-4e18-b3d9-c03d985dce34",
                  "tidsstempel": "2020-08-20T20:00:01.500",
                  "annullert": true,
                  "vurdering": {
                    "ident": "Z999999",
                    "epost": "ukjent@nav.no",
                    "tidspunkt": "2020-08-20T20:00:01.500",
                    "automatiskBehandling": false
                  }
                }
            ],
            "vedtaksperioder": [
              {
                "id": "2555046a-fad9-4c4f-9aba-de1721b2b497",
                "utbetalingId": "9b842ebe-699c-4a57-9265-453ca1ace142",
                "godkjentAv": "S123456",
                "godkjenttidspunkt": "2020-01-01T00:00:00.500",
                "automatiskBehandling": null
              },
              {
                "id": "ef991e12-b97c-4cf9-9f34-542dba36c582",
                "utbetalingId": "be9faa2a-d11e-4d63-926c-bc217692206f",
                "godkjentAv": "K123456",
                "godkjenttidspunkt": "2020-10-01T00:00:00.500",
                "automatiskBehandling": true
              },
              {
                "id": "290c176c-0a3a-4651-84c7-0bf32b62d193",
                "utbetalingId": "59b68a98-838b-440d-8f90-f8bfae2bc922"
              }
            ],
            "forkastede": [
              {
                "vedtaksperiode": {
                  "id": "7c253913-6b90-4435-af88-db2ee1909e6f",
                  "utbetalingId": "c11cdfbc-dcc4-4f29-9ef9-e0db7b197487",
                  "tilstand": "TIL_INFOTRYGD"
                }
              }
            ]
        }
    ],
    "skjemaVersjon": 55
}
"""
