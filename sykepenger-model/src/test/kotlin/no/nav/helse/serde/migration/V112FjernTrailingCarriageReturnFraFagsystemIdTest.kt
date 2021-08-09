package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V112FjernTrailingCarriageReturnFraFagsystemIdTest {
    @Test
    fun `Fjerner carriage return fra fagsystemId`() {
        assertEquals(toNode(expected), migrer(original))
    }

    private fun toNode(json: String) = serdeObjectMapper.readTree(json)

    private fun migrer(json: String) = listOf(V112FjernTrailingCarriageReturnFraFagsystemId()).migrate(toNode(json))

    @Language("JSON")
    private val original = """{

    "aktivitetslogg": {
        "aktiviteter": [
            {
                "detaljer": {
                    "linjer": [
                        {
                            "refFagsystemId": null
                        },
                        {
                            "refFagsystemId": "U5R43KXOGZDU5IYAHO6EM3H25Q\r\n"
                        }
                    ],
                    "fagsystemId": "U5R43KXOGZDU5IYAHO6EM3H25Q\r\n"
                }
            },
            {
                "detaljer": {
                    "fagsystemId": "U5R43KXOGZDU5IYAHO6EM3H25Q\r\n"
                }
            },
            {
                "detaljer": {}
            },
            {
                "detaljer": {
                    "noe": "ulinjet"
                }
            }
        ]
    },
    "arbeidsgivere": [
        {
            "utbetalinger": [
                {
                    "arbeidsgiverOppdrag": {
                        "linjer": [
                            {
                                "refFagsystemId": null
                            },
                            {
                                "refFagsystemId": "U5R43KXOGZDU5IYAHO6EM3H25A\r\n"
                            }
                        ],
                        "fagsystemId": "U5R43KXOGZDU5IYAHO6EM3H25Q\r\n"
                    },
                    "personOppdrag": {
                        "linjer": [],
                        "fagsystemId": "7JIIU3OCVVFBJANYSUJCWWFRIE\r\n"
                    }
                }
            ]
        }
    ],
    "skjemaVersjon": 111
}
"""

    @Language("JSON")
    private val expected = """{
    "aktivitetslogg": {
        "aktiviteter": [
            {
                "detaljer": {
                    "linjer": [
                        {
                            "refFagsystemId": null
                        },
                        {
                            "refFagsystemId": "U5R43KXOGZDU5IYAHO6EM3H25Q"
                        }
                    ],
                    "fagsystemId": "U5R43KXOGZDU5IYAHO6EM3H25Q"
                }
            },
            {
                "detaljer": {
                    "fagsystemId": "U5R43KXOGZDU5IYAHO6EM3H25Q"
                }
            },
            {
                "detaljer": {}
            },
            {
                "detaljer": {
                    "noe": "ulinjet"
                }
            }
        ]
    },
    "arbeidsgivere": [
        {
            "utbetalinger": [
                {
                    "arbeidsgiverOppdrag": {
                        "linjer": [
                            {
                                "refFagsystemId": null
                            },
                            {
                                "refFagsystemId": "U5R43KXOGZDU5IYAHO6EM3H25A"
                            }
                        ],
                        "fagsystemId": "U5R43KXOGZDU5IYAHO6EM3H25Q"
                    },
                    "personOppdrag": {
                        "linjer": [],
                        "fagsystemId": "7JIIU3OCVVFBJANYSUJCWWFRIE"
                    }
                }
            ]
        }
    ],
    "skjemaVersjon": 112
}
"""


}
