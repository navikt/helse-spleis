package no.nav.helse.serde.migration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class V3FjerneUtbetalingsreferanseFraArbeidsgiverTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    internal fun `fjerner utbetalingsreferanse fra arbeidsgiver`() {
        val json = objectMapper.readTree(personJson)
        listOf(V3FjerneUtbetalingsreferanseFraArbeidsgiver()).migrate(json)
        val migratedJson = json.toString()

        assertNotContains(migratedJson, "\"utbetalingsreferanse\":\"arbeidsgiver\"")
        assertContains(migratedJson, "\"utbetalingsreferanse\":\"vedtaksperiode\"")
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
      "utbetalingsreferanse": "arbeidsgiver",
      "vedtaksperioder": [
        {
          "utbetalingsreferanse": "vedtaksperiode"
        }
      ]
    }
  ]
}
"""
