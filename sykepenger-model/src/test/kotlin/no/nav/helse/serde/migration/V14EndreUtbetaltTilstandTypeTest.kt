package no.nav.helse.serde.migration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class V14EndreUtbetaltTilstandTypeTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `oversetter til nye tilstandtyper`() {
        val json = objectMapper.readTree(personJson)
        listOf(V14EndreUtbetaltTilstandType()).migrate(json)
        val migratedJson = json.toString()

        assertNotContains(migratedJson, "\"UTBETALT\"")
        assertContains(migratedJson, "\"AVSLUTTET\"")
        assertContains(migratedJson, "\"TIL_UTBETALING\"")
    }

    private fun assertNotContains(json: String, value: String) {
        assertFalse(json.contains(value)) { "Did not expect to find $value in $json" }
    }

    private fun assertContains(json: String, value: String) {
        assertTrue(json.contains(value)) { "Expected to find $value in $json" }
    }
}

private const val personJson = """
{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "tilstand": "UTBETALT"
        }, {
          "tilstand": "TIL_UTBETALING"
        }
      ]
    }
  ]
}
"""
