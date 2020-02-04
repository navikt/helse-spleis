package no.nav.helse.behov

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import no.nav.helse.person.ArbeidstakerHendelse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*

internal class BehovTest {
    @Test
    fun `Opprette ett nytt behov`() {
        val behov = Behov.nyttBehov(
            ArbeidstakerHendelse.Hendelsestype.Ytelser,
            listOf(Behovstype.Sykepengehistorikk, Behovstype.Foreldrepenger),
            "aktørid",
            "fnr",
            "orgnr",
            UUID.randomUUID(),
            mapOf("id" to "1123")
        )

        val json = behov.toJson()

        ObjectMapper().readTree(json)["@behov"].also {
            assertTrue(it.isArray)
            assertEquals(2, (it as ArrayNode).size())
        }

        assertTrue(json.contains("1123"))
    }
}

