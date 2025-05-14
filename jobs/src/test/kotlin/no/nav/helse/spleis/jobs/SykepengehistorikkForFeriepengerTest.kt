package no.nav.helse.spleis.jobs

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime
import java.time.Year
import java.util.UUID
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SykepengehistorikkForFeriepengerTest {

    @Test
    fun json() {
        val event = SykepengehistorikkForFeriepenger(
            fødselsnummer = "fnr",
            opptjeningsår = Year.of(2020)
        )
        val result = event.tilJson()
        val node = jacksonObjectMapper().readTree(result)

        assertDoesNotThrow { UUID.fromString(node.path("@id").asText()) }
        assertDoesNotThrow { LocalDateTime.parse(node.path("@opprettet").asText()) }
        assertEquals("behov", node.path("@event_name").asText())
        assertEquals(listOf("SykepengehistorikkForFeriepenger"), node.path("@behov").map(JsonNode::asText))
        assertEquals("2020-01-01", node.path("SykepengehistorikkForFeriepenger").path("historikkFom").asText())
        assertEquals("2020-12-31", node.path("SykepengehistorikkForFeriepenger").path("historikkTom").asText())

        assertEquals(
            setOf("fødselsnummer", "@event_name", "@id", "@opprettet", "@behov", "SykepengehistorikkForFeriepenger"),
            (node as ObjectNode).fieldNames().asSequence().toSet()
        )
    }
}
