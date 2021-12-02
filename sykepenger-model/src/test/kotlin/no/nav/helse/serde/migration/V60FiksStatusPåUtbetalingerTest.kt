package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V60FiksStatusPåUtbetalingerTest {

    @Test
    fun `retter IKKE_GODKJENT til UTBETALT hvis perioden var utbetalt`() {
        val migrated = listOf(V60FiksStatusPåUtbetalinger())
            .migrate(serdeObjectMapper.readTree(forkastetTilInfotrygdMedUtbetalingJson))
        val expected = serdeObjectMapper.readTree(expectedForkastetTilInfotrygdMedUtbetalingJson)

        assertEquals(expected, migrated)
    }

    @Test
    fun `setter riktig verdi for automatiskGodkjent`() {
        val migrated = listOf(V60FiksStatusPåUtbetalinger())
            .migrate(serdeObjectMapper.readTree(forkastetTilInfotrygdMedUtbetalingAutomatiskGodkjentJson))
        val expected = serdeObjectMapper.readTree(expectedForkastetTilInfotrygdMedUtbetalingAutomatiskGodkjentJson)

        assertEquals(expected, migrated)
    }
}

@Language("JSON")
private val forkastetTilInfotrygdMedUtbetalingJson =
    """{
    "aktivitetslogg": {
        "aktiviteter": [
            {
                "kontekster": [
                    1
                ],
                "alvorlighetsgrad": "INFO",
                "melding": "Utbetaling markert som godkjent av saksbehandler Doffen 2020-09-21T12:40:23.493751",
                "detaljer": {},
                "tidsstempel": "2020-11-21 12:40:57.014"
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
                    "vedtaksperiodeId": "7c253913-6b90-4435-af88-db2ee1909e6f"
                }
            }
        ]
    },
    "arbeidsgivere": [
        {
            "utbetalinger": [
                {
                    "id": "aaaaaaaa-dcc4-4f29-9ef9-e0db7b19748",
                    "tidsstempel": "2020-11-21T18:39:57.014",
                    "annullert": false,
                    "status": "IKKE_GODKJENT",
                    "vurdering": {
                        "ident": "Z999999",
                        "epost": "ukjent@nav.no",
                        "tidspunkt": "2020-11-21T18:39:57.014",
                        "automatiskBehandling": false
                    },
                    "type": "UTBETALING"
                },
                {
                    "id": "bbbbbbbb-dcc4-4f29-9ef9-e0db7b197487",
                    "tidsstempel": "2020-11-21T18:39:57.014",
                    "annullert": false,
                    "status": "IKKE_GODKJENT",
                    "vurdering": {
                        "ident": "Z999999",
                        "epost": "ukjent@nav.no",
                        "tidspunkt": "2020-11-21T18:39:57.014",
                        "automatiskBehandling": false
                    },
                    "type": "UTBETALING"
                }
            ],
            "vedtaksperioder": [],
            "forkastede": [
                {
                    "vedtaksperiode": {
                        "id": "7c253913-6b90-4435-af88-db2ee1909e6f",
                        "utbetalingId": "bbbbbbbb-dcc4-4f29-9ef9-e0db7b197487",
                        "tilstand": "TIL_INFOTRYGD"
                    }
                }
            ]
        }
    ],
    "skjemaVersjon": 59
}
"""


@Language("JSON")
private val expectedForkastetTilInfotrygdMedUtbetalingJson =
    """
{
  "aktivitetslogg": {
    "aktiviteter": [
      {
        "kontekster": [
          1
        ],
        "alvorlighetsgrad": "INFO",
        "melding": "Utbetaling markert som godkjent av saksbehandler Doffen 2020-09-21T12:40:23.493751",
        "detaljer": {},
        "tidsstempel": "2020-11-21 12:40:57.014"
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
          "vedtaksperiodeId": "7c253913-6b90-4435-af88-db2ee1909e6f"
        }
      }
    ]
  },
    "arbeidsgivere": [
        {
            "utbetalinger": [
                {
                    "id": "aaaaaaaa-dcc4-4f29-9ef9-e0db7b19748",
                    "tidsstempel": "2020-11-21T18:39:57.014",
                    "annullert": false,
                    "status": "IKKE_GODKJENT",
                    "vurdering": {
                        "ident": "Z999999",
                        "epost": "ukjent@nav.no",
                        "tidspunkt": "2020-11-21T18:39:57.014",
                        "automatiskBehandling": false
                    },
                    "type": "UTBETALING"
                },
                {
                  "id": "bbbbbbbb-dcc4-4f29-9ef9-e0db7b197487",
                  "tidsstempel": "2020-11-21T18:39:57.014",
                  "annullert": false,
                  "status": "UTBETALT",
                  "vurdering": {
                      "ident": "Z999999",
                      "epost": "ukjent@nav.no",
                      "tidspunkt": "2020-11-21T18:39:57.014",
                      "automatiskBehandling": false
                  },
                  "type": "UTBETALING"
                }
            ],
            "vedtaksperioder": [],
            "forkastede": [
              {
                "vedtaksperiode": {
                  "id": "7c253913-6b90-4435-af88-db2ee1909e6f",
                  "utbetalingId": "bbbbbbbb-dcc4-4f29-9ef9-e0db7b197487",
                  "tilstand": "TIL_INFOTRYGD"
                }
              }
            ]
        }
    ],
    "skjemaVersjon": 60
}
"""

@Language("JSON")
private val forkastetTilInfotrygdMedUtbetalingAutomatiskGodkjentJson =
    """{
    "aktivitetslogg": {
        "aktiviteter": [
            {
                "kontekster": [
                    1
                ],
                "alvorlighetsgrad": "INFO",
                "melding": "Utbetaling markert som godkjent automatisk 2020-09-21T12:40:23.493751",
                "detaljer": {},
                "tidsstempel": "2020-11-21 12:40:57.014"
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
                    "vedtaksperiodeId": "7c253913-6b90-4435-af88-db2ee1909e6f"
                }
            }
        ]
    },
    "arbeidsgivere": [
        {
            "utbetalinger": [
                {
                    "id": "bbbbbbbb-dcc4-4f29-9ef9-e0db7b197487",
                    "tidsstempel": "2020-11-21T18:39:57.014",
                    "annullert": false,
                    "status": "IKKE_GODKJENT",
                    "vurdering": {
                        "ident": "Z999999",
                        "epost": "ukjent@nav.no",
                        "tidspunkt": "2020-11-21T18:39:57.014",
                        "automatiskBehandling": false
                    },
                    "type": "UTBETALING"
                }
            ],
            "vedtaksperioder": [],
            "forkastede": [
                {
                    "vedtaksperiode": {
                        "id": "7c253913-6b90-4435-af88-db2ee1909e6f",
                        "utbetalingId": "bbbbbbbb-dcc4-4f29-9ef9-e0db7b197487",
                        "tilstand": "TIL_INFOTRYGD"
                    }
                }
            ]
        }
    ],
    "skjemaVersjon": 59
}
"""


@Language("JSON")
private val expectedForkastetTilInfotrygdMedUtbetalingAutomatiskGodkjentJson =
    """
{
  "aktivitetslogg": {
    "aktiviteter": [
      {
        "kontekster": [
          1
        ],
        "alvorlighetsgrad": "INFO",
        "melding": "Utbetaling markert som godkjent automatisk 2020-09-21T12:40:23.493751",
        "detaljer": {},
        "tidsstempel": "2020-11-21 12:40:57.014"
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
          "vedtaksperiodeId": "7c253913-6b90-4435-af88-db2ee1909e6f"
        }
      }
    ]
  },
    "arbeidsgivere": [
        {
            "utbetalinger": [
                {
                  "id": "bbbbbbbb-dcc4-4f29-9ef9-e0db7b197487",
                  "tidsstempel": "2020-11-21T18:39:57.014",
                  "annullert": false,
                  "status": "UTBETALT",
                  "vurdering": {
                      "ident": "Z999999",
                      "epost": "ukjent@nav.no",
                      "tidspunkt": "2020-11-21T18:39:57.014",
                      "automatiskBehandling": true
                  },
                  "type": "UTBETALING"
                }
            ],
            "vedtaksperioder": [],
            "forkastede": [
              {
                "vedtaksperiode": {
                  "id": "7c253913-6b90-4435-af88-db2ee1909e6f",
                  "utbetalingId": "bbbbbbbb-dcc4-4f29-9ef9-e0db7b197487",
                  "tilstand": "TIL_INFOTRYGD"
                }
              }
            ]
        }
    ],
    "skjemaVersjon": 60
}
"""
