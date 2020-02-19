package no.nav.helse.serde

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory

internal fun List<JsonMigration>.migrate(jsonNode: JsonNode) = JsonMigration.migrate(this, jsonNode)

// Understands a specific version of a JSON message
// Implements GoF Command Pattern to perform migration
internal abstract class JsonMigration(private val version: Int) {
    internal companion object {
        private val log = LoggerFactory.getLogger(JsonMigration::class.java)
        private const val SkjemaversjonKey = "skjemaVersjon"
        private const val InitialVersion = 0

        internal fun migrate(migrations: List<JsonMigration>, jsonNode: JsonNode) =
            migrations.sortedBy { it.version }.forEach { it.migrate(jsonNode) }

        internal fun medSkjemaversjon(migrations: List<JsonMigration>, jsonNode: JsonNode) = jsonNode.apply {
            (jsonNode as ObjectNode).put(SkjemaversjonKey, gjeldendeVersjon(migrations))
        }

        internal fun skjemaVersjon(jsonNode: JsonNode) =
            jsonNode.path(SkjemaversjonKey).asInt(InitialVersion)

        internal fun gjeldendeVersjon(migrations: List<JsonMigration>) =
            migrations.maxBy { it.version }?.version ?: InitialVersion
    }

    init {
        require(version > InitialVersion) { "Ugyldig versjon for migrering. Må være større enn $InitialVersion" }
    }

    fun migrate(jsonNode: JsonNode) {
        if (jsonNode !is ObjectNode) return
        if (!shouldMigrate(jsonNode)) return
        doMigration(jsonNode)
        after(jsonNode)
    }

    protected abstract fun doMigration(jsonNode: ObjectNode)

    protected open fun shouldMigrate(jsonNode: JsonNode) =
        skjemaVersjon(jsonNode) < version

    private fun after(jsonNode: ObjectNode) {
        log.info("Successfully migrated json to $version")
        jsonNode.put(SkjemaversjonKey, version)
    }
}
