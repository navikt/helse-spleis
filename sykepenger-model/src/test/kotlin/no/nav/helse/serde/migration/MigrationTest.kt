package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.readResource
import no.nav.helse.serde.serdeObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.skyscreamer.jsonassert.JSONAssert

internal abstract class MigrationTest(private val migration: JsonMigration) {
    open fun meldingerSupplier() = MeldingerSupplier.empty

    protected fun toNode(json: String): JsonNode = serdeObjectMapper.readTree(json)
    protected fun migrer(json: String) = listOf(migration).migrate(toNode(json), meldingerSupplier())

    protected fun assertMigration(expectedJson: String, originalJson: String) {
        assertMigrationRaw(expectedJson.readResource(), originalJson.readResource())
    }

    protected fun assertMigrationRaw(expectedJson: String, originalJson: String) {
        val expected = toNode(expectedJson)
        val migrert = migrer(originalJson)
        JSONAssert.assertEquals("\n$expected\n$migrert\n", expected.toString(), migrert.toString(), true)
        assertEquals(expected, migrert)
    }
}
