package no.nav.helse.spleis.e2e

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.spleis.meldinger.TestRapid
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode.STRICT

internal object KontraktAssertions {

    internal fun TestRapid.assertUtgåendeMelding(
        forventetMelding: String,
        faktiskMelding: (aktuelle: List<JsonNode>) -> JsonNode = { it.last() },
        førAssertEquals: (faktiskMelding: ObjectNode) -> Unit
    ): ObjectNode {
        val eventName = objectMapper.readTree(forventetMelding).path("@event_name").asText()
        val event = faktiskMelding(inspektør.meldinger(eventName)) as ObjectNode
        val kopi = event.deepCopy()
        event.assertOgFjernStandardfelter()
        førAssertEquals(event)
        JSONAssert.assertEquals(forventetMelding, event.toString(), STRICT)
        return kopi
    }

    internal fun TestRapid.assertAntallUtgåendeMeldinger(eventName: String, forventetAntall: Int) {
        assertEquals(forventetAntall, inspektør.meldinger(eventName).size)
    }

    private fun ObjectNode.assertOgFjernStandardfelter() {
        assertOgFjernUUID("@id")
        assertOgFjernLocalDateTime("@opprettet")
        assertOgFjern("@forårsaket_av") { check(it.isObject) }
        assertOgFjern("system_read_count") { check(it.isInt) }
        assertOgFjern("system_participating_services") { check(it.isArray) }
    }

    internal fun ObjectNode.assertOgFjernUUID(key: String) = assertOgFjern(key) { UUID.fromString(it.asText()) }
    internal fun ObjectNode.assertOgFjernLocalDateTime(key: String) = assertOgFjern(key) { LocalDateTime.parse(it.asText()) }
    private fun ObjectNode.assertOgFjern(key: String, validation:(value: JsonNode) -> Unit) {
        assertDoesNotThrow { validation(path(key)) }
        remove(key)
    }

    private val objectMapper = jacksonObjectMapper()
}