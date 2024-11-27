package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.serde.migration.JsonMigration.Companion.skjemaVersjon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.UUID

internal class JsonMigrationTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `henter bare meldinger én gang`() {
        var invocationCount = 0
        val supplier = {
            invocationCount += 1
            mapOf(UUID.randomUUID() to Hendelse(UUID.randomUUID(), "", LocalDateTime.now()))
        }
        val meldinger = mutableListOf<Map<UUID, Hendelse>>()
        listOf(
            object : JsonMigration(1) {
                override val description = ""

                override fun doMigration(
                    jsonNode: ObjectNode,
                    meldingerSupplier: MeldingerSupplier
                ) {
                    meldinger.add(meldingerSupplier.hentMeldinger())
                }
            },
            object : JsonMigration(2) {
                override val description = ""

                override fun doMigration(
                    jsonNode: ObjectNode,
                    meldingerSupplier: MeldingerSupplier
                ) {
                    meldinger.add(meldingerSupplier.hentMeldinger())
                }
            }
        ).migrate(objectMapper.createObjectNode(), supplier)

        assertEquals(1, invocationCount)
        assertEquals(2, meldinger.size)
        assertEquals(meldinger.first(), meldinger.last())
    }

    @Test
    fun `kan migrere JSON uten skjemaversjon`() {
        val version = 1
        val expectedField = "field_1"
        val expectedValue = "value"

        val migratedJson =
            objectMapper
                .readTree("{}")
                .migrate(AddFieldMigration(version, expectedField, expectedValue))

        assertEquals(version, skjemaVersjon(migratedJson))
        assertEquals(expectedValue, migratedJson[expectedField].textValue())
    }

    @Test
    fun `kan migrere flere ganger`() {
        val version1 = 1
        val version2 = 2
        val field1 = "field_1"
        val value1 = "value1"
        val field2 = "field_2"
        val value2 = "value2"

        val migratedJson =
            objectMapper
                .readTree("{}")
                .migrate(AddFieldMigration(version1, field1, value1))
                .migrate(AddFieldMigration(version2, field2, value2))

        assertEquals(version2, skjemaVersjon(migratedJson))
        assertEquals(value1, migratedJson[field1].textValue())
        assertEquals(value2, migratedJson[field2].textValue())
    }

    @Test
    fun `kan ikke migrere til mindre versjoner`() {
        val version1 = 1
        val version2 = 2
        val field1 = "field_1"
        val value1 = "value1"
        val field2 = "field_2"
        val value2 = "value2"

        val migratedJson =
            objectMapper
                .readTree("{}")
                .migrate(AddFieldMigration(version2, field2, value2))
                .migrate(AddFieldMigration(version1, field1, value1))

        assertEquals(version2, skjemaVersjon(migratedJson))
        assertFalse(migratedJson.has(field1))
        assertEquals(value2, migratedJson[field2].textValue())
    }

    @Test
    fun `migreringer kjøres i sortert rekkefølge`() {
        val version1 = 1
        val version2 = 2
        val field1 = "field_1"
        val value1 = "value1"
        val field2 = "field_2"
        val value2 = "value2"

        val migrations =
            listOf(
                AddFieldMigration(version2, field2, value2),
                AddFieldMigration(version1, field1, value1)
            )

        val migratedJson = migrations.migrate(objectMapper.readTree("{}"))

        assertEquals(version2, skjemaVersjon(migratedJson))
        assertEquals(value1, migratedJson[field1].textValue())
        assertEquals(value2, migratedJson[field2].textValue())
    }

    @Test
    fun `versjoner må være unike`() {
        val migrations =
            listOf(
                AddFieldMigration(1, "foo", "bar"),
                AddFieldMigration(1, "foo", "bar")
            )

        assertThrows<IllegalArgumentException> { migrations.migrate(objectMapper.readTree("{}")) }
    }

    private fun JsonNode.migrate(migration: JsonMigration) = listOf(migration).migrate(this)

    private class AddFieldMigration(
        version: Int,
        private val field: String,
        private val value: String
    ) : JsonMigration(version) {
        override val description = "Test migration"

        override fun doMigration(
            jsonNode: ObjectNode,
            meldingerSupplier: MeldingerSupplier
        ) {
            jsonNode.put(field, value)
        }
    }
}
