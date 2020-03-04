package no.nav.helse.serde.migration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class V13FjernerGradFraArbeidsgiverperiodeDagTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    internal fun leggerTilKontekstMap() {
        val json = objectMapper.readTree(json)
        listOf(V13FjernerGradFraArbeidsgiverperiodeDag()).migrate(json)
        val migratedJson = json.toString()

        assertNotContains(migratedJson, "\"grad\"")
        assertContains(migratedJson, "\"type\":\"ArbeidsgiverperiodeDag\"")
    }

    private fun assertNotContains(json: String, value: String) {
        assertFalse(json.contains(value)) { "Did not expect to find $value in $json" }
    }

    private fun assertContains(json: String, value: String) {
        assertTrue(json.contains(value)) { "Expected to find $value in $json" }
    }

    private val json = """
{
    "arbeidsgivere": [
        {
            "utbetalingstidslinjer": [
                {
                    "dager": [
                        {
                            "type": "ArbeidsgiverperiodeDag",
                            "inntekt": 1400,
                            "dato": "2018-01-01",
                            "grad": 100.0
                        }
                    ]
                }
            ]
        }
    ]
}
"""


}
