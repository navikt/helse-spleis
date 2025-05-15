package no.nav.helse.spleis.jobs

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.util.*
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SykepengehistorikkForFeriepengerTest {

    @Test
    fun json() {
        val event = SykepengehistorikkForFeriepenger(
            fødselsnummer = "fnr",
            opptjeningsår = Year.of(2020),
            datoForSisteFeriepengekjøringIInfotrygd = LocalDate.of(2021, 5, 10)
        )
        val result = event.tilJson()
        val node = jacksonObjectMapper().readTree(result)

        assertDoesNotThrow { UUID.fromString(node.path("@id").asText()) }
        assertDoesNotThrow { LocalDateTime.parse(node.path("@opprettet").asText()) }
        assertEquals("behov", node.path("@event_name").asText())
        assertEquals(listOf("SykepengehistorikkForFeriepenger"), node.path("@behov").map(JsonNode::asText))
        assertEquals("2020-01-01", node.path("SykepengehistorikkForFeriepenger").path("historikkFom").asText())
        assertEquals("2020-12-31", node.path("SykepengehistorikkForFeriepenger").path("historikkTom").asText())
        assertEquals("2021-05-10", node.path("SykepengehistorikkForFeriepenger").path("datoForSisteFeriepengekjøringIInfotrygd").asText())

        assertEquals(
            setOf("fødselsnummer", "@event_name", "@id", "@opprettet", "@behov", "SykepengehistorikkForFeriepenger"),
            (node as ObjectNode).fieldNames().asSequence().toSet()
        )
    }
}
