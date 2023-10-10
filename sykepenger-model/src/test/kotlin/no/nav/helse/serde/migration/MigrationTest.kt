package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
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
        val migrert = migrer(originalJson)
        val expected = erstattPlaceholders(toNode(expectedJson), migrert)
        println(migrert.toString())
        assertJson(migrert.toString(), expected.toString(), jsonCompareMode)
        return migrert
    }

    // bytter ut <!.......!> på felter med verdi fra actual. På den måten kan man asserte at feltet finnes, men verdien er ukjent.
    // Eksempelvis om migreringen innfører en Random UUID på et felt i en migrering, så kan man i expected-json skrive:
    // { "id": "<! en random UUID legges på her !>" }
    private fun erstattPlaceholders(expected: JsonNode, actual: JsonNode): JsonNode {
        when (expected) {
            is ArrayNode -> {
                if (actual is ArrayNode) {
                    expected.forEachIndexed { index, value -> erstattPlaceholders(value, actual.path(index)) }
                }
            }
            is ObjectNode -> {
                if (actual is ObjectNode) {
                    expected.fields().forEach { (key, value) ->
                        when (value) {
                            is TextNode -> {
                                if (value.asText().matches(placeholderRegex)) {
                                    val valueNode = actual.path(key)
                                    expected.put(key, valueNode.asText())
                                }
                            }

                            is ArrayNode, is ObjectNode -> erstattPlaceholders(value, actual.path(key))
                        }
                    }
                }
            }
        }
        return expected
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

    private companion object {
        private val placeholderRegex = "<!.*!>".toRegex()
    }
}
