package no.nav.helse.serde.migration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V14NettoBeløpIVedtaksperiodeTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `setter netto beløp til 0 som default`() {
        val json = objectMapper.readTree(personJson)
        listOf(V14NettoBeløpIVedtaksperiode()).migrate(json)
        val migratedJson = objectMapper.readTree(json.toString())
        val expected = objectMapper.readTree(expectedPersonJson)
        assertEquals(expected, migratedJson)
    }
}

@Language("JSON")
private const val personJson = """{
    "arbeidsgivere": [
        {
            "vedtaksperioder": [
                {},
                {}
            ]
        }
    ]
}
"""

@Language("JSON")
private const val expectedPersonJson = """{
    "arbeidsgivere": [
        {
            "vedtaksperioder": [
                {
                    "personNettoBeløp": 0,
                    "arbeidsgiverNettoBeløp": 0
                },
                {
                    "personNettoBeløp": 0,
                    "arbeidsgiverNettoBeløp": 0
                }
            ]
        }
    ],
    "skjemaVersjon": 14
}
"""
