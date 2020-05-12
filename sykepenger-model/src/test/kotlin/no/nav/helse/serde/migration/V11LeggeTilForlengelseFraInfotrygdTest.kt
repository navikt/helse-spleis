package no.nav.helse.serde.migration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class V11LeggeTilForlengelseFraInfotrygdTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `legger til forlengelseFraInfotrygd`() {
        val json = objectMapper.readTree(oldJson)
        listOf(V11LeggeTilForlengelseFraInfotrygd()).migrate(json)
        val migratedJson = objectMapper.readTree(json.toString())
        assertEquals(objectMapper.readTree(expectedJson), migratedJson)
    }
}

@Language("JSON")
private const val oldJson = """
{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [{}]
    }
  ]
}
"""

@Language("JSON")
private const val expectedJson = """
{
  "skjemaVersjon": 11,
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
            "forlengelseFraInfotrygd": "IKKE_ETTERSPURT"
        }
      ]
    }
  ]
}
"""
