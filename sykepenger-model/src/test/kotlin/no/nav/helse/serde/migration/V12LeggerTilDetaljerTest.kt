package no.nav.helse.serde.migration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class V12LeggerTilDetaljerTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    internal fun leggerTilKontekstMap() {
        val json = objectMapper.readTree(json)
        listOf(V12LeggerTilDetaljer()).migrate(json)
        val migratedJson = json.toString()

        assertContains(migratedJson, "\"detaljer\":{}")
    }

    private fun assertContains(json: String, value: String) {
        assertTrue(json.contains(value)) { "Expected to find $value in $json" }
    }

    private val json = """
{
  "aktivitetslogg": {
    "aktiviteter": [
      {
        "alvorlighetsgrad": "NEED",
        "melding": "Trenger noe"
      }
    ]
  }
}
"""
}
