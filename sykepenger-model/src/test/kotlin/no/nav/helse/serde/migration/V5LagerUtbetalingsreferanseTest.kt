package no.nav.helse.serde.migration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.utbetalingstidslinje.genererUtbetalingsreferanse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

internal class V5LagerUtbetalingsreferanseTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    internal fun `fjerner utbetalingsreferanse fra arbeidsgiver`() {
        val json = objectMapper.readTree(personJson)
        listOf(V5LagerUtbetalingsreferanse()).migrate(json)
        val migratedJson = json.toString()

        val expcetedUtbetalingsreferanse = genererUtbetalingsreferanse(vedtaksperiodeId)

        assertNotContains(migratedJson, "\"utbetalingsreferanse\":null")
        assertContains(migratedJson, "\"utbetalingsreferanse\":\"should_not_change\"")
        assertContains(migratedJson, "\"utbetalingsreferanse\":\"$expcetedUtbetalingsreferanse\"")
    }

    private fun assertNotContains(json: String, value: String) {
        assertFalse(json.contains(value)) { "Did not expect to find $value in $json" }
    }

    private fun assertContains(json: String, value: String) {
        assertTrue(json.contains(value)) { "Expected to find $value in $json" }
    }
}

private val vedtaksperiodeId = UUID.randomUUID()

private val personJson = """
{
  "arbeidsgivere": [
    {
      "vedtaksperioder": [
        {
          "id": "1426a4b8-0b5a-455d-8389-6909ab94d56e",
          "utbetalingsreferanse": "should_not_change"
        }, {
          "id": "$vedtaksperiodeId",
          "utbetalingsreferanse": null
        }
      ]
    }
  ]
}
"""
