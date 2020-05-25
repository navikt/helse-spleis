package no.nav.helse.serde.migration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V16StatusIUtbetalingTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `setter status på utbetaling basert på tilstand til vedtaksperiode`() {
        val json = objectMapper.readTree(personJson)
        listOf(
            V16StatusIUtbetaling()
        ).migrate(json)
        val migratedJson = objectMapper.readTree(json.toString())
        val expected = objectMapper.readTree(expectedPersonJson)
        assertEquals(expected, migratedJson)
    }
}

@Language("JSON")
private const val personJson = """{
    "aktivitetslogg": {
        "aktiviteter": [
            {
                "melding": "Invaliderer vedtaksperiode: 00000000-0000-0000-0000-000000000003 på grunn av annullering",
                "tidsstempel": "2020-05-26 07:42:00.908"
            }
        ]
    },
    "arbeidsgivere": [
        {
            "utbetalinger": [
                {
                    "utbetalingstidslinje": {
                        "dager": []
                    },
                    "tidsstempel": "2020-05-26T07:42:00.330356"
                },
                {
                    "utbetalingstidslinje": {
                        "dager": [
                            {
                                "dato": "2020-01-01"
                            },
                            {
                                "dato": "2020-01-02"
                            },
                            {
                                "dato": "2020-01-03"
                            }
                        ]
                    },
                    "tidsstempel": "2020-05-26T07:42:00.330356"
                },
                {
                    "utbetalingstidslinje": {
                        "dager": [
                            {
                                "dato": "2020-01-01"
                            },
                            {
                                "dato": "2020-01-02"
                            },
                            {
                                "dato": "2020-01-03"
                            }
                        ]
                    },
                    "tidsstempel": "2020-05-25T07:42:00.330356"
                },
                {
                    "utbetalingstidslinje": {
                        "dager": [
                            {
                                "dato": "2020-01-01"
                            },
                            {
                                "dato": "2020-01-02"
                            }
                        ]
                    },
                    "tidsstempel": "2020-05-26T07:42:00.330356"
                },
                {
                    "utbetalingstidslinje": {
                        "dager": [
                            {
                                "dato": "2020-01-01"
                            }
                        ]
                    },
                    "tidsstempel": "2020-05-26T07:42:00.330356"
                }
            ],
            "vedtaksperioder": [
                {
                    "id": "00000000-0000-0000-0000-000000000000",
                    "tilstand": "AVSLUTTET_UTEN_UTBETALING",
                    "utbetalingstidslinje": {
                        "dager": []
                    }
                },
                {
                    "id": "00000000-0000-0000-0000-000000000001",
                    "tilstand": "AVSLUTTET",
                    "utbetalingstidslinje": {
                        "dager": [
                            {
                                "dato": "2020-01-01"
                            }
                        ]
                    }
                },
                {
                    "id": "00000000-0000-0000-0000-000000000002",
                    "tilstand": "AVVENTER_GODKJENNING",
                    "utbetalingstidslinje": {
                        "dager": [
                            {
                                "dato": "2020-01-02"
                            }
                        ]
                    }
                },
                {
                    "id": "00000000-0000-0000-0000-000000000003",
                    "tilstand": "TIL_INFOTRYGD",
                    "utbetalingstidslinje": {
                        "dager": [
                            {
                                "dato": "2020-01-03"
                            }
                        ]
                    }
                }
            ]
        }
    ]
}
"""

@Language("JSON")
private const val expectedPersonJson = """{
    "aktivitetslogg": {
        "aktiviteter": [
            {
                "melding": "Invaliderer vedtaksperiode: 00000000-0000-0000-0000-000000000003 på grunn av annullering",
                "tidsstempel": "2020-05-26 07:42:00.908"
            }
        ]
    },
    "arbeidsgivere": [
        {
            "utbetalinger": [
                {
                    "utbetalingstidslinje": {
                        "dager": []
                    },
                    "tidsstempel": "2020-05-26T07:42:00.330356",
                    "status": "IKKE_UTBETALT"
                },
                {
                    "utbetalingstidslinje": {
                        "dager": [
                            {
                                "dato": "2020-01-01"
                            },
                            {
                                "dato": "2020-01-02"
                            },
                            {
                                "dato": "2020-01-03"
                            }
                        ]
                    },
                    "tidsstempel": "2020-05-26T07:42:00.330356",
                    "status": "ANNULLERT"
                },
                {
                    "utbetalingstidslinje": {
                        "dager": [
                            {
                                "dato": "2020-01-01"
                            },
                            {
                                "dato": "2020-01-02"
                            },
                            {
                                "dato": "2020-01-03"
                            }
                        ]
                    },
                    "tidsstempel": "2020-05-25T07:42:00.330356",
                    "status": "UTBETALT"
                },
                {
                    "utbetalingstidslinje": {
                        "dager": [
                            {
                                "dato": "2020-01-01"
                            },
                            {
                                "dato": "2020-01-02"
                            }
                        ]
                    },
                    "tidsstempel": "2020-05-26T07:42:00.330356",
                    "status": "IKKE_UTBETALT"
                },
                {
                    "utbetalingstidslinje": {
                        "dager": [
                            {
                                "dato": "2020-01-01"
                            }
                        ]
                    },
                    "tidsstempel": "2020-05-26T07:42:00.330356",
                    "status": "UTBETALT"
                }
            ],
            "vedtaksperioder": [
                {
                    "id": "00000000-0000-0000-0000-000000000000",
                    "tilstand": "AVSLUTTET_UTEN_UTBETALING",
                    "utbetalingstidslinje": {
                        "dager": []
                    }
                },
                {
                    "id": "00000000-0000-0000-0000-000000000001",
                    "tilstand": "AVSLUTTET",
                    "utbetalingstidslinje": {
                        "dager": [
                            {
                                "dato": "2020-01-01"
                            }
                        ]
                    }
                },
                {
                    "id": "00000000-0000-0000-0000-000000000002",
                    "tilstand": "AVVENTER_GODKJENNING",
                    "utbetalingstidslinje": {
                        "dager": [
                            {
                                "dato": "2020-01-02"
                            }
                        ]
                    }
                },
                {
                    "id": "00000000-0000-0000-0000-000000000003",
                    "tilstand": "TIL_INFOTRYGD",
                    "utbetalingstidslinje": {
                        "dager": [
                            {
                                "dato": "2020-01-03"
                            }
                        ]
                    }
                }
            ]
        }
    ],
    "skjemaVersjon": 16
}
"""
