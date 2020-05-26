package no.nav.helse.serde.migration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V17ForkastedePerioderTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `legger til forkastede perioder`() {
        val json = objectMapper.readTree(ingenForkastedeJson)
        listOf(
            V17ForkastedePerioder()
        ).migrate(json)
        val migratedJson = objectMapper.readTree(json.toString())
        val expected = objectMapper.readTree(expectedIngenForkastedeJson)
        assertEquals(expected, migratedJson)
    }

    @Test
    fun `flytter ikke avsluttede perioder til forkastede`() {
        val json = objectMapper.readTree(enAvsluttetPeriodeJson)
        listOf(
            V17ForkastedePerioder()
        ).migrate(json)
        val migratedJson = objectMapper.readTree(json.toString())
        val expected = objectMapper.readTree(expectedEnAvsluttetPeriodeJson)
        assertEquals(expected, migratedJson)
    }

    @Test
    fun `bare forkastede`() {
        val json = objectMapper.readTree(bareForkastedeJson)
        listOf(
            V17ForkastedePerioder()
        ).migrate(json)
        val migratedJson = objectMapper.readTree(json.toString())
        val expected = objectMapper.readTree(expectedBareForkastedeJson)
        assertEquals(expected, migratedJson)
    }

    @Test
    fun `en forkastet`() {
        val json = objectMapper.readTree(enForkastetJson)
        listOf(
            V17ForkastedePerioder()
        ).migrate(json)
        val migratedJson = objectMapper.readTree(json.toString())
        val expected = objectMapper.readTree(expectedEnForkastetJson)
        assertEquals(expected, migratedJson)
    }
}

@Language("JSON")
private const val bareForkastedeJson = """{
    "arbeidsgivere": [
        {
            "vedtaksperioder": [
                {
                    "tilstand": "AVSLUTTET_UTEN_UTBETALING"
                },
                {
                    "tilstand": "AVVENTER_GODKJENNING"
                },
                {
                    "tilstand": "TIL_INFOTRYGD"
                }
            ]
        }
    ]
}
"""

@Language("JSON")
private const val expectedBareForkastedeJson = """{
    "arbeidsgivere": [
        {
            "vedtaksperioder": [],
            "forkastede": [
                {
                    "tilstand": "AVSLUTTET_UTEN_UTBETALING"
                },
                {
                    "tilstand": "AVVENTER_GODKJENNING"
                },
                {
                    "tilstand": "TIL_INFOTRYGD"
                }
            ]
        }
    ],
    "skjemaVersjon": 17
}
"""

@Language("JSON")
private const val enAvsluttetPeriodeJson = """{
    "arbeidsgivere": [
        {
            "vedtaksperioder": [
                {
                    "tilstand": "AVVENTER_GODKJENNING"
                },
                {
                    "tilstand": "TIL_INFOTRYGD"
                },
                {
                    "tilstand": "AVSLUTTET"
                }
            ]
        }
    ]
}
"""

@Language("JSON")
private const val expectedEnAvsluttetPeriodeJson = """{
    "arbeidsgivere": [
        {
            "vedtaksperioder": [
                {
                    "tilstand": "AVSLUTTET"
                }
            ],
            "forkastede": [
                {
                    "tilstand": "AVVENTER_GODKJENNING"
                },
                {
                    "tilstand": "TIL_INFOTRYGD"
                }
            ]
        }
    ],
    "skjemaVersjon": 17
}
"""

@Language("JSON")
private const val ingenForkastedeJson = """{
    "arbeidsgivere": [
        {
            "vedtaksperioder": [
                {
                    "tilstand": "AVSLUTTET"
                }
            ]
        }
    ]
}
"""

@Language("JSON")
private const val expectedIngenForkastedeJson = """{
    "arbeidsgivere": [
        {
            "vedtaksperioder": [
                {
                    "tilstand": "AVSLUTTET"
                }
            ],
            "forkastede": []
        }
    ],
    "skjemaVersjon": 17
}
"""

@Language("JSON")
private const val enForkastetJson = """{
    "arbeidsgivere": [
        {
            "vedtaksperioder": [
                {
                    "tilstand": "TIL_INFOTRYGD"
                }
            ]
        }
    ]
}
"""

@Language("JSON")
private const val expectedEnForkastetJson = """{
    "arbeidsgivere": [
        {
            "vedtaksperioder": [],
            "forkastede": [
                {
                    "tilstand": "TIL_INFOTRYGD"
                }
            ]
        }
    ],
    "skjemaVersjon": 17
}
"""
