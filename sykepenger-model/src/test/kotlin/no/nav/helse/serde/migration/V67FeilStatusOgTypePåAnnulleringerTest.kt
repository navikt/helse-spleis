package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V67FeilStatusOgTypePåAnnulleringerTest {

    @Test
    fun `endrer status og tilstand på annullering med en linje`() {
        val expected = serdeObjectMapper.readTree(expectedEnOpphørtLinje)
        val migrated = listOf(V67FeilStatusOgTypePåAnnulleringer())
            .migrate(serdeObjectMapper.readTree(enOpphørtLinje))
        assertEquals(expected, migrated)
    }

    @Test
    fun `endrer status og tilstand på annullering med flere linjer`() {
        val expected = serdeObjectMapper.readTree(expectedFlereLinjer)
        val migrated = listOf(V67FeilStatusOgTypePåAnnulleringer())
            .migrate(serdeObjectMapper.readTree(flereLinjer))
        assertEquals(expected, migrated)
    }

    @Test
    fun `endrer ikke status ved flere linjer og første != OPPH`() {
        val expected = serdeObjectMapper.readTree(expectedFlereLinjerFørsteErIkkeOpphJson)
        val migrated = listOf(V67FeilStatusOgTypePåAnnulleringer())
            .migrate(serdeObjectMapper.readTree(flereLinjerFørsteErIkkeOpphJson))
        assertEquals(expected, migrated)
    }

    @Test
    fun `migrerer ikke tomme linjer`() {
        val expected = serdeObjectMapper.readTree(expectedTommeLinjerJson)
        val migrated = listOf(V67FeilStatusOgTypePåAnnulleringer())
            .migrate(serdeObjectMapper.readTree(tommeLinjerJson))
        assertEquals(expected, migrated)
    }

    @Test
    fun `endrer ikke status om type er feil`() {
        val expected = serdeObjectMapper.readTree(expectedFeilStatusJson)
        val migrated = listOf(V67FeilStatusOgTypePåAnnulleringer())
            .migrate(serdeObjectMapper.readTree(feilStatusJson))
        assertEquals(expected, migrated)
    }
}

@Language("JSON")
private val flereLinjer =
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
                                "fom": "2020-09-22",
                                "tom": "2020-09-23",
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
private val enOpphørtLinje =
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
    "skjemaVersjon": 64
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
    "skjemaVersjon": 64
}
"""

@Language("JSON")
private val flereLinjerFørsteErIkkeOpphJson =
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
                                "tom": "2020-09-30",
                                "datoStatusFom": null,
                                "statuskode": "ENDR"
                            },
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
    "skjemaVersjon": 64
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
    "skjemaVersjon": 64
}
"""

@Language("JSON")
private val expectedFlereLinjer =
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
                                "fom": "2020-09-22",
                                "tom": "2020-09-23",
                                "datoStatusFom": null,
                                "statuskode": "ENDR"
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
    "skjemaVersjon": 67
}
"""

@Language("JSON")
private val expectedEnOpphørtLinje =
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
    "skjemaVersjon": 67
}
"""

@Language("JSON")
private val expectedFlereLinjerFørsteErIkkeOpphJson =
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
                                "tom": "2020-09-30",
                                "datoStatusFom": null,
                                "statuskode": "ENDR"
                            },
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
    "skjemaVersjon": 67
}
"""

@Language("JSON")
private val expectedTommeLinjerJson =
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
    "skjemaVersjon": 67
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
    "skjemaVersjon": 67
}
"""
