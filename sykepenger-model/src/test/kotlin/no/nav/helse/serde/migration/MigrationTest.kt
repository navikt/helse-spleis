package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID
import no.nav.helse.readResource
import no.nav.helse.serde.serdeObjectMapper
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

internal abstract class MigrationTest(private val migration: () -> JsonMigration) {

    internal constructor(migration: JsonMigration) : this({ migration })

    internal val observatør = TestJsonMigrationObserver()
    open fun meldingerSupplier() = MeldingerSupplier.empty

    protected fun toNode(json: String): JsonNode = serdeObjectMapper.readTree(json)
    protected fun migrer(json: String) = listOf(migration()).migrate(toNode(json), meldingerSupplier(), observatør)

    protected fun assertMigration(
        expectedJson: String,
        originalJson: String,
        jsonCompareMode: JSONCompareMode = JSONCompareMode.STRICT
    ) {
        assertMigrationRaw(expectedJson.readResource(), originalJson.readResource(), jsonCompareMode)
    }

    protected fun assertMigrationRaw(
        expectedJson: String,
        originalJson: String,
        jsonCompareMode: JSONCompareMode = JSONCompareMode.STRICT
    ) {
        val expected = toNode(expectedJson)
        val migrert = migrer(originalJson)
        JSONAssert.assertEquals(
            "\n$expected\n$migrert\n",
            expected.toString(),
            migrert.toString(),
            jsonCompareMode
        )
    }

    internal class TestJsonMigrationObserver: JsonMigrationObserver {
        val slettedeVedtaksperioder = mutableListOf<Pair<UUID, JsonNode>>()
        val endredeVedtaksperioder = mutableListOf<Map<String, String>>()
        override fun vedtaksperiodeSlettet(vedtaksperiodeId: UUID, vedtaksperiodeNode: JsonNode) {
            slettedeVedtaksperioder.add(vedtaksperiodeId to vedtaksperiodeNode)
        }

        override fun vedtaksperiodeEndret(vedtaksperiodeId: UUID, gammelTilstand: String, nyTilstand: String) {
            endredeVedtaksperioder.add(mapOf(
                "id" to vedtaksperiodeId.toString(),
                "gammelTilstand" to gammelTilstand,
                "nyTilstand" to nyTilstand
            ))
        }
    }
}
