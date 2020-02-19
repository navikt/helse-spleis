package no.nav.helse.serde.migration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class V2EndreTilstandTyperTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `oversetter til nye tilstandtyper`() {
        val json = objectMapper.readTree(personJson)
        listOf(V2EndreTilstandTyper()).migrate(json)
        val migratedJson = json.toString()

        assertNotContains(migratedJson, "\"MOTTATT_NY_SØKNAD\"")
        assertNotContains(migratedJson, "\"AVVENTER_SENDT_SØKNAD\"")
        assertContains(migratedJson, "\"MOTTATT_SYKMELDING\"")
        assertContains(migratedJson, "\"AVVENTER_SØKNAD\"")
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
          "tilstand": "MOTTATT_NY_SØKNAD"
        }, {
          "tilstand": "AVVENTER_SENDT_SØKNAD"
        }
      ]
    }
  ]
}
"""
