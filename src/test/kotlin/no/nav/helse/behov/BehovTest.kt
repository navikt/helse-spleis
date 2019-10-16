package no.nav.helse.behov

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class BehovTest {
    @Test
    fun `Opprette ett nytt behov`() {
        val behov = Behov.nyttBehov("sykepengehistorikk", mapOf("id" to "1123"))

        val json = behov.toJson()
        assertTrue(json.contains("sykepengehistorikk"))
        assertTrue(json.contains("1123"))

    }

    @Test
    fun `Det er ikke lov å lage et invalid behov`() {
        assertThrows<IllegalArgumentException> { Behov.fromJson("{}") }
    }


    @Test
    fun `En behovsløser må kunne opprette et behov fra json, og legge på løsning, og lage json`() {

        val orignalBehov = Behov.nyttBehov("sykepengehistorikk", mapOf("id" to "1123"))


        val behov = Behov.fromJson(orignalBehov.toJson())
        assertFalse(behov.harLøsning())
        behov.løsBehov(mapOf<String, Any>(
                "aktørId" to "234"

        ))
        assertTrue(behov.harLøsning())

        val json = behov.toJson()
        assertTrue(json.contains("aktørId"))
        assertTrue(json.contains("234"))

    }
}

