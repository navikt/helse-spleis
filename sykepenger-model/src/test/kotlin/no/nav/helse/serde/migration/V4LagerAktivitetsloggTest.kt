package no.nav.helse.serde.migration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class V4LagerAktivitetsloggTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `Legger til aktivitetslogg`() {
        val json = objectMapper.readTree(personJson)
        listOf(V4LagerAktivitetslogg()).migrate(json)
        val migratedJson = json.toString()

        assertContains(migratedJson, "\"aktivitetslogg\"")
        assertContains(migratedJson, "\"aktiviteter\"")
    }

    private fun assertContains(json: String, value: String) {
        assertTrue(json.contains(value)) { "Expected to find $value in $json" }
    }
}

private const val personJson = """
{
    "aktørId" : "12020052345",
    "fødselsnummer" : "42"
}
"""
