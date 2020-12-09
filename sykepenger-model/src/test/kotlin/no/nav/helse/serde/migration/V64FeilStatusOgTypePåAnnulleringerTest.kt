package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V64FeilStatusOgTypePåAnnulleringerTest {
    @Test
    fun `endrer status og tilstand på annullering`() {
        val expected = serdeObjectMapper.readTree(expectedJson)
        val migrated = listOf(V64FeilStatusOgTypePåAnnulleringer())
            .migrate(serdeObjectMapper.readTree(originalJson))
        assertEquals(expected, migrated)
    }

    @Test
    fun `endrer ikke status om linjer != 1`() {
        val expected = serdeObjectMapper.readTree(expectedFlereLinjerJson)
        val migrated = listOf(V64FeilStatusOgTypePåAnnulleringer())
            .migrate(serdeObjectMapper.readTree(flereLinjerJson))
        assertEquals(expected, migrated)

        val expected1 = serdeObjectMapper.readTree(expectedOriginalTommeLinjerJson)
        val migrated1 = listOf(V64FeilStatusOgTypePåAnnulleringer())
            .migrate(serdeObjectMapper.readTree(tommeLinjerJson))
        assertEquals(expected1, migrated1)
    }

    @Test
    fun `endrer ikke status om type er feil`() {
        val expected = serdeObjectMapper.readTree(expectedFeilStatusJson)
        val migrated = listOf(V64FeilStatusOgTypePåAnnulleringer())
            .migrate(serdeObjectMapper.readTree(feilStatusJson))
        assertEquals(expected, migrated)
    }
}

@Language("JSON")
private val originalJson =
    """
{
    "arbeidsgivere": [
        {
            "utbetalinger": [
                {
                    "id": "1FCA4168-CA23-4D85-AE69-CCDE400287EF",
                    "arbeidsgiverOppdrag": {
                        "linjer": [
                            {
                                "fom": "2020-09-15",
                                "tom": "2020-09-21",
                                "datoStatusFom": "2020-05-13",
                                "statuskode": "OPPH"
                            }
                        ],
                        "fagsystemId": "AKWJDAKWDJAWKDJAA",
                        "endringskode": "ENDR"
                    },
                    "status": "UTBETALT",
                    "type": "UTBETALING"
                }
            ]
        }
    ],
    "skjemaVersjon": 63
}
"""

@Language("JSON")
private val feilStatusJson =
    """
{
    "arbeidsgivere": [
        {
            "utbetalinger": [
                {
                    "id": "1FCA4168-CA23-4D85-AE69-CCDE400287EF",
                    "arbeidsgiverOppdrag": {
                        "linjer": [
                            {
                                "fom": "2020-09-15",
                                "tom": "2020-09-21",
                                "datoStatusFom": "2020-05-13",
                                "statuskode": "OPPH"
                            }
                        ],
                        "fagsystemId": "AKWJDAKWDJAWKDJAA",
                        "endringskode": "ENDR"
                    },
                    "status": "UTBETALT",
                    "type": "ANNULLERING"
                }
            ]
        }
    ],
    "skjemaVersjon": 63
}
"""

@Language("JSON")
private val flereLinjerJson =
    """
{
    "arbeidsgivere": [
        {
            "utbetalinger": [
                {
                    "id": "1FCA4168-CA23-4D85-AE69-CCDE400287EF",
                    "arbeidsgiverOppdrag": {
                        "linjer": [
                            {
                                "fom": "2020-09-15",
                                "tom": "2020-09-21",
                                "datoStatusFom": "2020-05-13",
                                "statuskode": "OPPH"
                            },
                            {
                                "fom": "2020-09-15",
                                "tom": "2020-09-30",
                                "datoStatusFom": null,
                                "statuskode": "ENDR"
                            }
                        ],
                        "fagsystemId": "AKWJDAKWDJAWKDJAA",
                        "endringskode": "ENDR"
                    },
                    "status": "UTBETALT",
                    "type": "UTBETALING"
                }
            ]
        }
    ],
    "skjemaVersjon": 63
}
"""

@Language("JSON")
private val tommeLinjerJson =
    """
{
    "arbeidsgivere": [
        {
            "utbetalinger": [
                {
                    "id": "1FCA4168-CA23-4D85-AE69-CCDE400287EF",
                    "arbeidsgiverOppdrag": {
                        "linjer": [],
                        "fagsystemId": "AKWJDAKWDJAWKDJAA",
                        "endringskode": "ENDR"
                    },
                    "status": "UTBETALT",
                    "type": "UTBETALING"
                }
            ]
        }
    ],
    "skjemaVersjon": 63
}
"""

@Language("JSON")
private val expectedJson =
    """
{
    "arbeidsgivere": [
        {
            "utbetalinger": [
                {
                    "id": "1FCA4168-CA23-4D85-AE69-CCDE400287EF",
                    "arbeidsgiverOppdrag": {
                        "linjer": [
                            {
                                "fom": "2020-09-15",
                                "tom": "2020-09-21",
                                "datoStatusFom": "2020-05-13",
                                "statuskode": "OPPH"
                            }
                        ],
                        "fagsystemId": "AKWJDAKWDJAWKDJAA",
                        "endringskode": "ENDR"
                    },
                    "status": "FORKASTET",
                    "type": "ANNULLERING"
                }
            ]
        }
    ],
    "skjemaVersjon": 64
}
"""

@Language("JSON")
private val expectedFlereLinjerJson =
    """
{
    "arbeidsgivere": [
        {
            "utbetalinger": [
                {
                    "id": "1FCA4168-CA23-4D85-AE69-CCDE400287EF",
                    "arbeidsgiverOppdrag": {
                        "linjer": [
                            {
                                "fom": "2020-09-15",
                                "tom": "2020-09-21",
                                "datoStatusFom": "2020-05-13",
                                "statuskode": "OPPH"
                            },
                            {
                                "fom": "2020-09-15",
                                "tom": "2020-09-30",
                                "datoStatusFom": null,
                                "statuskode": "ENDR"
                            }
                        ],
                        "fagsystemId": "AKWJDAKWDJAWKDJAA",
                        "endringskode": "ENDR"
                    },
                    "status": "UTBETALT",
                    "type": "UTBETALING"
                }
            ]
        }
    ],
    "skjemaVersjon": 64
}
"""

@Language("JSON")
private val expectedOriginalTommeLinjerJson =
    """
{
    "arbeidsgivere": [
        {
            "utbetalinger": [
                {
                    "id": "1FCA4168-CA23-4D85-AE69-CCDE400287EF",
                    "arbeidsgiverOppdrag": {
                        "linjer": [],
                        "fagsystemId": "AKWJDAKWDJAWKDJAA",
                        "endringskode": "ENDR"
                    },
                    "status": "UTBETALT",
                    "type": "UTBETALING"
                }
            ]
        }
    ],
    "skjemaVersjon": 64
}
"""

@Language("JSON")
private val expectedFeilStatusJson =
    """
{
    "arbeidsgivere": [
        {
            "utbetalinger": [
                {
                    "id": "1FCA4168-CA23-4D85-AE69-CCDE400287EF",
                    "arbeidsgiverOppdrag": {
                        "linjer": [
                            {
                                "fom": "2020-09-15",
                                "tom": "2020-09-21",
                                "datoStatusFom": "2020-05-13",
                                "statuskode": "OPPH"
                            }
                        ],
                        "fagsystemId": "AKWJDAKWDJAWKDJAA",
                        "endringskode": "ENDR"
                    },
                    "status": "UTBETALT",
                    "type": "ANNULLERING"
                }
            ]
        }
    ],
    "skjemaVersjon": 64
}
"""
