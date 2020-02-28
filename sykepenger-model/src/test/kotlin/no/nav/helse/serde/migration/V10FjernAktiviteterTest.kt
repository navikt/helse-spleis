package no.nav.helse.serde.migration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class V10FjernAktiviteterTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    internal fun leggerTilKontekstMap() {
        val json = objectMapper.readTree(json)
        listOf(V10FjernAktiviteter()).migrate(json)
        val migratedJson = json.toString()

        assertNotContains(migratedJson, "\"kontekster\"")
        assertContains(migratedJson, "\"aktiviteter\":[]")
    }

    private fun assertNotContains(json: String, value: String) {
        assertFalse(json.contains(value)) { "Did not expect to find $value in $json" }
    }

    private fun assertContains(json: String, value: String) {
        assertTrue(json.contains(value)) { "Expected to find $value in $json" }
    }

    private val json = """
{
  "aktivitetslogg": {
    "aktiviteter": [
      {
        "kontekster": [
          {
            "kontekstType": "Sykmelding",
            "melding": "foo"
          },
          {
            "kontekstType": "Person",
            "melding": "bar"
          }
        ]
      }
    ]
  }
}
"""


}
