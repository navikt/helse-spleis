package no.nav.helse.spleis.mediator.e2e

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.spleis.mediator.meldinger.TestRapid
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode.STRICT

internal object KontraktAssertions {

    internal fun TestRapid.assertUtgåendeMelding(
        forventetMelding: String,
        faktiskMelding: (aktuelle: List<JsonNode>) -> JsonNode = { it.last() },
        førAssertEquals: (faktiskMelding: ObjectNode) -> Unit = { }
    ): ObjectNode {
        val forventetJson = objectMapper.readTree(forventetMelding) as ObjectNode
        val eventName = forventetJson.path("@event_name").asText()
        val faktiskJson = faktiskMelding(inspektør.meldinger(eventName)) as ObjectNode
        val kopi = faktiskJson.deepCopy()
        faktiskJson.assertOgFjernStandardfelter()
        (faktiskJson to forventetJson).let {
            it.assertOgFjernUUIDTemplates()
            it.assertOgFjernLocalDateTimeTemplates()
        }
        førAssertEquals(faktiskJson)
        JSONAssert.assertEquals(forventetJson.toString(), faktiskJson.toString(), STRICT)
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

    private fun Pair<ObjectNode, ObjectNode>.assertOgFjernTemplates(template: String, assertOgFjern: (faktiskJson: ObjectNode, key: String) -> Unit) {
        val (faktiskJson, forventetJson) = this
        val uuidTemplates = forventetJson.properties().filter { it.value.asText() == template }.map { it.key }
        uuidTemplates.forEach {
            forventetJson.remove(it)
            assertOgFjern(faktiskJson, it)
        }
    }
    private fun Pair<ObjectNode, ObjectNode>.assertOgFjernUUIDTemplates() = assertOgFjernTemplates("<uuid>") { faktiskJson, key ->
        faktiskJson.assertOgFjernUUID(key)
    }

    private fun Pair<ObjectNode, ObjectNode>.assertOgFjernLocalDateTimeTemplates() = assertOgFjernTemplates("<timestamp>") { faktiskJson, key ->
        faktiskJson.assertOgFjernLocalDateTime(key)
    }

    private fun ObjectNode.assertOgFjernUUID(key: String) = assertOgFjern(key) { UUID.fromString(it.asText()) }
    private fun ObjectNode.assertOgFjernLocalDateTime(key: String) = assertOgFjern(key) { LocalDateTime.parse(it.asText()) }
    internal fun ObjectNode.assertOgFjern(key: String, validation:(value: JsonNode) -> Unit) {
        if (!key.contains(".")) {
            assertDoesNotThrow({ validation(path(key))}, "$key er ikke på forventet format!")
            remove(key)
            return
        }
        val sisteKey = key.split(".").last()
        val objekt = key.substringBeforeLast(".").split(".").fold(this as JsonNode) { result, nestedKey -> result.path(nestedKey) } as ObjectNode
        assertDoesNotThrow { validation(objekt.path(sisteKey)) }
        objekt.remove(sisteKey)
    }

    private val objectMapper = jacksonObjectMapper()
}