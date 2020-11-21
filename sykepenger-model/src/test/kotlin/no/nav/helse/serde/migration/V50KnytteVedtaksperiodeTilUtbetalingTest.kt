package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V50KnytteVedtaksperiodeTilUtbetalingTest {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `lager id på utbetalinger`() {
        val migrated = listOf(V50KnytteVedtaksperiodeTilUtbetaling())
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
          12,
          12,
          1,
          2,
          3,
          11
        ],
        "alvorlighetsgrad": "INFO",
        "melding": "Utbetalingslinjer bygget vellykket",
        "detaljer": {},
        "tidsstempel": "2020-11-21T18:39:57.014"
      },
      {
        "kontekster": [
          12,
          12,
          1,
          2,
          3,
          11
        ],
        "alvorlighetsgrad": "INFO",
        "melding": "Noe skjedde",
        "detaljer": {},
        "tidsstempel": "2020-11-21T18:39:57.014"
      },
      {
        "kontekster": [
          12,
          12,
          1,
          2,
          4,
          11
        ],
        "alvorlighetsgrad": "INFO",
        "melding": "Ingen utbetalingslinjer bygget",
        "detaljer": {},
        "tidsstempel": "2020-11-20T20:00:00.000"
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
        "kontekstType": "Person",
        "kontekstMap": {
          "fødselsnummer": "12020052345",
          "aktørId": "42"
        }
      },
      {
        "kontekstType": "Arbeidsgiver",
        "kontekstMap": {
          "organisasjonsnummer": "987654321"
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
      },
      {
        "kontekstType": "Tilstand",
        "kontekstMap": {
          "tilstand": "MOTTATT_SYKMELDING_FERDIG_GAP"
        }
      },
      {
        "kontekstType": "Søknad",
        "kontekstMap": {
          "aktørId": "12345",
          "fødselsnummer": "12020052345",
          "organisasjonsnummer": "987654321",
          "id": "cdf276d0-8fbd-4ff7-b9d7-5a84bdc6463d"
        }
      },
      {
        "kontekstType": "Tilstand",
        "kontekstMap": {
          "tilstand": "AVVENTER_GAP"
        }
      },
      {
        "kontekstType": "Inntektsmelding",
        "kontekstMap": {
          "aktørId": "aktørId",
          "fødselsnummer": "12020052345",
          "organisasjonsnummer": "987654321",
          "id": "c0c4f24d-9d93-4522-bee1-a5d26b0ac5be"
        }
      },
      {
        "kontekstType": "Tilstand",
        "kontekstMap": {
          "tilstand": "AVVENTER_VILKÅRSPRØVING_GAP"
        }
      },
      {
        "kontekstType": "Vilkårsgrunnlag",
        "kontekstMap": {
          "aktørId": "aktørId",
          "fødselsnummer": "12020052345",
          "organisasjonsnummer": "987654321",
          "id": "b8742f66-5930-43e1-91fe-30259c6fa4d1"
        }
      },
      {
        "kontekstType": "Tilstand",
        "kontekstMap": {
          "tilstand": "AVVENTER_HISTORIKK"
        }
      },
      {
        "kontekstType": "Ytelser",
        "kontekstMap": {
          "aktørId": "aktørId",
          "fødselsnummer": "12020052345",
          "organisasjonsnummer": "987654321",
          "id": "f703ef84-9b64-435b-9428-392718771dde"
        }
      },
      {
        "kontekstType": "Tilstand",
        "kontekstMap": {
          "tilstand": "AVVENTER_SIMULERING"
        }
      }
    ]
  },
    "arbeidsgivere": [
        {
            "utbetalinger": [
                {
                  "id": "c11cdfbc-dcc4-4f29-9ef9-e0db7b197487",
                  "tidsstempel": "2020-11-21T18:39:57.114"
                },
                {
                  "id": "9b842ebe-699c-4a57-9265-453ca1ace142",
                  "tidsstempel": "2020-11-20T20:00:01.000"
                },
                {
                  "id": "be9faa2a-d11e-4d63-926c-bc217692206f",
                  "tidsstempel": "2020-10-20T20:00:01.000"
                }
            ],
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
                "id": "7c253913-6b90-4435-af88-db2ee1909e6f"
              }
            ]
        }
    ],
    "skjemaVersjon": 49
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
          12,
          12,
          1,
          2,
          3,
          11
        ],
        "alvorlighetsgrad": "INFO",
        "melding": "Utbetalingslinjer bygget vellykket",
        "detaljer": {},
        "tidsstempel": "2020-11-21T18:39:57.014"
      },
      {
        "kontekster": [
          12,
          12,
          1,
          2,
          3,
          11
        ],
        "alvorlighetsgrad": "INFO",
        "melding": "Noe skjedde",
        "detaljer": {},
        "tidsstempel": "2020-11-21T18:39:57.014"
      },
      {
        "kontekster": [
          12,
          12,
          1,
          2,
          4,
          11
        ],
        "alvorlighetsgrad": "INFO",
        "melding": "Ingen utbetalingslinjer bygget",
        "detaljer": {},
        "tidsstempel": "2020-11-20T20:00:00.000"
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
        "kontekstType": "Person",
        "kontekstMap": {
          "fødselsnummer": "12020052345",
          "aktørId": "42"
        }
      },
      {
        "kontekstType": "Arbeidsgiver",
        "kontekstMap": {
          "organisasjonsnummer": "987654321"
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
      },
      {
        "kontekstType": "Tilstand",
        "kontekstMap": {
          "tilstand": "MOTTATT_SYKMELDING_FERDIG_GAP"
        }
      },
      {
        "kontekstType": "Søknad",
        "kontekstMap": {
          "aktørId": "12345",
          "fødselsnummer": "12020052345",
          "organisasjonsnummer": "987654321",
          "id": "cdf276d0-8fbd-4ff7-b9d7-5a84bdc6463d"
        }
      },
      {
        "kontekstType": "Tilstand",
        "kontekstMap": {
          "tilstand": "AVVENTER_GAP"
        }
      },
      {
        "kontekstType": "Inntektsmelding",
        "kontekstMap": {
          "aktørId": "aktørId",
          "fødselsnummer": "12020052345",
          "organisasjonsnummer": "987654321",
          "id": "c0c4f24d-9d93-4522-bee1-a5d26b0ac5be"
        }
      },
      {
        "kontekstType": "Tilstand",
        "kontekstMap": {
          "tilstand": "AVVENTER_VILKÅRSPRØVING_GAP"
        }
      },
      {
        "kontekstType": "Vilkårsgrunnlag",
        "kontekstMap": {
          "aktørId": "aktørId",
          "fødselsnummer": "12020052345",
          "organisasjonsnummer": "987654321",
          "id": "b8742f66-5930-43e1-91fe-30259c6fa4d1"
        }
      },
      {
        "kontekstType": "Tilstand",
        "kontekstMap": {
          "tilstand": "AVVENTER_HISTORIKK"
        }
      },
      {
        "kontekstType": "Ytelser",
        "kontekstMap": {
          "aktørId": "aktørId",
          "fødselsnummer": "12020052345",
          "organisasjonsnummer": "987654321",
          "id": "f703ef84-9b64-435b-9428-392718771dde"
        }
      },
      {
        "kontekstType": "Tilstand",
        "kontekstMap": {
          "tilstand": "AVVENTER_SIMULERING"
        }
      }
    ]
  },
    "arbeidsgivere": [
        {
            "utbetalinger": [
                {
                  "id": "c11cdfbc-dcc4-4f29-9ef9-e0db7b197487",
                  "tidsstempel": "2020-11-21T18:39:57.114"
                },
                {
                  "id": "9b842ebe-699c-4a57-9265-453ca1ace142",
                  "tidsstempel": "2020-11-20T20:00:01.000"
                },
                {
                  "id": "be9faa2a-d11e-4d63-926c-bc217692206f",
                  "tidsstempel": "2020-10-20T20:00:01.000"
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
                "id": "7c253913-6b90-4435-af88-db2ee1909e6f",
                "utbetalingId": "9b842ebe-699c-4a57-9265-453ca1ace142"
              }
            ]
        }
    ],
    "skjemaVersjon": 50
}
"""
