package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory

internal fun List<JsonMigration>.migrate(jsonNode: JsonNode) =
    JsonMigration.migrate(this, jsonNode)

// Implements GoF Command Pattern to perform migration
internal abstract class JsonMigration(private val version: Int) {
    internal companion object {
        private val log = LoggerFactory.getLogger(JsonMigration::class.java)
        private const val SkjemaversjonKey = "skjemaVersjon"
        private const val InitialVersion = 0

        internal fun migrate(migrations: List<JsonMigration>, jsonNode: JsonNode) = jsonNode.apply {
            migrations.sortedBy { it.version }.also {
                require(it.groupBy { it.version }.filterValues { it.size > 1 }.isEmpty()) { "Versjoner må være unike" }
                it.forEach { it.migrate(this) }
            }
        }

        internal fun medSkjemaversjon(migrations: List<JsonMigration>, jsonNode: JsonNode) = jsonNode.apply {
            (jsonNode as ObjectNode).put(
                SkjemaversjonKey,
                gjeldendeVersjon(migrations)
            )
        }

        internal fun skjemaVersjon(jsonNode: JsonNode) =
            jsonNode.path(SkjemaversjonKey).asInt(
                InitialVersion
            )

        internal fun gjeldendeVersjon(migrations: List<JsonMigration>) =
            migrations.maxOfOrNull { it.version } ?: InitialVersion
    }

    init {
        require(version > InitialVersion) { "Ugyldig versjon for migrering. Må være større enn $InitialVersion" }
    }

    protected abstract val description: String

    private fun migrate(jsonNode: JsonNode) {
        if (jsonNode !is ObjectNode) return
        if (!shouldMigrate(jsonNode)) return
        doMigration(jsonNode)
        after(jsonNode)
    }

    protected abstract fun doMigration(jsonNode: ObjectNode)

    protected open fun shouldMigrate(jsonNode: JsonNode) =
        skjemaVersjon(jsonNode) < version

    private fun after(jsonNode: ObjectNode) {
        log.info("Successfully migrated json to $version using ${this.javaClass.name}: ${this.description}")
        jsonNode.put(SkjemaversjonKey, version)
    }
}
