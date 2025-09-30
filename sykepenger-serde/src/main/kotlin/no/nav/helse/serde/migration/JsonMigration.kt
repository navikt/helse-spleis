package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.serde.serdeObjectMapper
import org.slf4j.LoggerFactory

fun interface MeldingerSupplier {
    companion object {
        internal val empty = MeldingerSupplier { emptyMap() }
    }

    fun hentMeldinger(): Map<UUID, Hendelse>
}

data class Hendelse(val meldingsreferanseId: UUID, val meldingstype: String, val lestDato: LocalDateTime)

internal fun List<JsonMigration>.migrate(
    skjemaVersjon: Int,
    unmigratedJson: String,
    meldingerSupplier: MeldingerSupplier = MeldingerSupplier.empty
) =
    JsonMigration.migrate(this, skjemaVersjon, unmigratedJson, MemoizedMeldingerSupplier(meldingerSupplier))

private class MemoizedMeldingerSupplier(private val supplier: MeldingerSupplier) : MeldingerSupplier {
    private val meldinger: Map<UUID, Hendelse> by lazy { supplier.hentMeldinger() }

    override fun hentMeldinger(): Map<UUID, Hendelse> = meldinger
}

// Implements GoF Command Pattern to perform migration
internal abstract class JsonMigration(private val version: Int) {
    internal companion object {
        private val log = LoggerFactory.getLogger(JsonMigration::class.java)
        private const val SkjemaversjonKey = "skjemaVersjon"
        private const val InitialVersion = 0

        internal val String.dato get() = LocalDate.parse(this)
        internal val String.uuid get() = UUID.fromString(this)

        internal fun migrate(
            migrations: List<JsonMigration>,
            skjemaVersjon: Int,
            unmigratedJson: String,
            supplier: MeldingerSupplier
        ): Pair<Int, String> {
            val sortedMigrations = migrations.sortedBy { it.version }
            require(sortedMigrations.windowed(2).none { (a, b) -> a.version == b.version }) { "Versjoner må være unike" }
            return sortedMigrations.fold(skjemaVersjon to unmigratedJson) { (inputSkjemaversjon, inputJson), migration ->
                migration.migrate(inputSkjemaversjon, inputJson, supplier)
            }
        }

        internal fun skjemaVersjon(jsonNode: JsonNode) =
            jsonNode.path(SkjemaversjonKey).asInt(InitialVersion)

        internal fun gjeldendeVersjon(migrations: List<JsonMigration>) =
            migrations.maxOfOrNull { it.version } ?: InitialVersion
    }

    init {
        require(version > InitialVersion) { "Ugyldig versjon for migrering. Må være større enn $InitialVersion" }
    }

    protected abstract val description: String

    private fun migrate(skjemaVersjon: Int, unmigratedJson: String, meldingerSupplier: MeldingerSupplier): Pair<Int, String> {
        if (!shouldMigrate(skjemaVersjon)) return (skjemaVersjon to unmigratedJson)
        val jsonNode = serdeObjectMapper.readTree(unmigratedJson)
        require(jsonNode is ObjectNode) { "Kan kun migrere ObjectNodes" }
        doMigration(jsonNode, meldingerSupplier)
        after(jsonNode)
        val migratedJson = jsonNode.toString()
        return version to migratedJson
    }

    protected abstract fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier)

    protected open fun shouldMigrate(skjemaVersjon: Int) =
        skjemaVersjon < version

    private fun after(jsonNode: ObjectNode) {
        log.info("Successfully migrated json to $version using ${this.javaClass.name}: ${this.description}")
        jsonNode.put(SkjemaversjonKey, version)
    }
}
