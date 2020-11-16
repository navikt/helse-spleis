package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V46GamleAnnulleringsforsøkTest {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `setter annullert true`() {
        val migrated = listOf(V46GamleAnnulleringsforsøk())
            .migrate(objectMapper.readTree(originalJson()))
        val expected = objectMapper.readTree(expectedJson())

        assertEquals(expected, migrated)
    }
}

@Language("JSON")
private fun originalJson() =
    """
{
    "fødselsnummer": "fnr",
    "arbeidsgivere": [
        {
            "utbetalinger": [
                {
                    "arbeidsgiverOppdrag": {
                        "fagsystemId": "fagsystemId-2",
                        "linjer": [
                            {
                                "datoStatusFom": null,
                                "statuskode": null
                            },
                            {
                                "datoStatusFom": null,
                                "statuskode": null
                            }
                        ],
                        "endringskode": "ENDR"
                    },
                    "status": "IKKE_UTBETALT",
                    "annullert": false
                },
                {
                    "arbeidsgiverOppdrag": {
                        "fagsystemId": "fagsystemId-1",
                        "linjer": [
                            {
                                "datoStatusFom": "2020-05-25",
                                "statuskode": "OPPH"
                            }
                        ]
                    },
                    "status": "UTBETALT",
                    "annullert": false
                },
                {
                    "arbeidsgiverOppdrag": {
                        "fagsystemId": "fagsystemId-3",
                        "linjer": [
                            {
                                "datoStatusFom": null,
                                "statuskode": null
                            }
                        ]
                    },
                    "status": "IKKE_UTBETALT",
                    "annullert": false
                },
                {
                    "arbeidsgiverOppdrag": {
                        "fagsystemId": "fagsystemId-4",
                        "linjer": [
                            {
                                "datoStatusFom": null,
                                "statuskode": null
                            }
                        ]
                    },
                    "status": "IKKE_UTBETALT",
                    "annullert": false
                }
            ]
        }
    ],
    "skjemaVersjon": 45
}
"""

@Language("JSON")
private fun expectedJson() =
    """
{
    "fødselsnummer": "fnr",
    "arbeidsgivere": [
        {
            "utbetalinger": [
                {
                    "arbeidsgiverOppdrag": {
                        "fagsystemId": "fagsystemId-2",
                        "linjer": [
                            {
                                "datoStatusFom": null,
                                "statuskode": null
                            },
                            {
                                "datoStatusFom": null,
                                "statuskode": null
                            }
                        ],
                        "endringskode": "ENDR"
                    },
                    "status": "IKKE_UTBETALT",
                    "annullert": false
                },
                {
                    "arbeidsgiverOppdrag": {
                        "fagsystemId": "fagsystemId-1",
                        "linjer": [
                            {
                                "datoStatusFom": "2020-05-25",
                                "statuskode": "OPPH"
                            }
                        ]
                    },
                    "status": "UTBETALT",
                    "annullert": true
                },
                {
                    "arbeidsgiverOppdrag": {
                        "fagsystemId": "fagsystemId-3",
                        "linjer": [
                            {
                                "datoStatusFom": null,
                                "statuskode": null
                            }
                        ]
                    },
                    "status": "IKKE_UTBETALT",
                    "annullert": false
                },
                {
                    "arbeidsgiverOppdrag": {
                        "fagsystemId": "fagsystemId-4",
                        "linjer": [
                            {
                                "datoStatusFom": null,
                                "statuskode": null
                            }
                        ]
                    },
                    "status": "IKKE_UTBETALT",
                    "annullert": false
                }
            ]
        }
    ],
    "skjemaVersjon": 46
}
"""
