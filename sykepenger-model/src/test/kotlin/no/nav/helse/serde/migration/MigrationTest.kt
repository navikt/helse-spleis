package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.readResource
import no.nav.helse.serde.serdeObjectMapper
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

internal abstract class MigrationTest(private val migration: () -> JsonMigration) {

    internal constructor(migration: JsonMigration) : this({ migration })

    open fun meldingerSupplier() = MeldingerSupplier.empty

    protected fun toNode(json: String): JsonNode = serdeObjectMapper.readTree(json)
    protected fun migrer(json: String): JsonNode {
        val migers = migration()
        return listOf(migers).migrate(toNode(json), meldingerSupplier())
    }

    protected fun assertMigration(
        expectedJson: String,
        originalJson: String,
        jsonCompareMode: JSONCompareMode = JSONCompareMode.STRICT
    ) = assertMigrationRaw(expectedJson.readResource(), originalJson.readResource(), jsonCompareMode)

    protected fun assertMigrationRaw(
        expectedJson: String,
        originalJson: String,
        jsonCompareMode: JSONCompareMode = JSONCompareMode.STRICT
    ): JsonNode {
        val expected = toNode(expectedJson)
        val migrert = migrer(originalJson)
        println(migrert.toString())
        assertJson(migrert.toString(), expected.toString(), jsonCompareMode)
        return migrert
    }

    protected fun assertJson(
        actual: String,
        expected: String,
        jsonCompareMode: JSONCompareMode = JSONCompareMode.STRICT
    ) {
        JSONAssert.assertEquals(
            "\n$expected\n$actual\n",
            expected,
            actual,
            jsonCompareMode
        )
    }
}
