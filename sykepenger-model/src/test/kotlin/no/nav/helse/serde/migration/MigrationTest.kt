package no.nav.helse.serde.migration

import no.nav.helse.readResource
import no.nav.helse.serde.serdeObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals

internal abstract class MigrationTest(
    private val migration: JsonMigration) {

    private fun toNode(json: String) = serdeObjectMapper.readTree(json)
    private fun migrer(json: String) = listOf(migration).migrate(toNode(json))

    protected fun assertMigration(expectedJson: String, originalJson: String) {
        assertMigrationRaw(expectedJson.readResource(), originalJson.readResource())
    }

    protected fun assertMigrationRaw(expectedJson: String, originalJson: String) {
        assertEquals(toNode(expectedJson), migrer(originalJson))
    }
}
