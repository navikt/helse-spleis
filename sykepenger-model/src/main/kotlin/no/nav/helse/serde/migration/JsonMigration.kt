package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory
import java.util.*

fun interface MeldingerSupplier {
    companion object {
        internal val empty = MeldingerSupplier { emptyMap() }
    }
    fun hentMeldinger(): Map<UUID, Pair<Navn, Json>>
}

typealias Navn = String
typealias Json = String

internal fun List<JsonMigration>.migrate(
    jsonNode: JsonNode,
    meldingerSupplier: MeldingerSupplier = MeldingerSupplier.empty,
    observer: JsonMigrationObserver = JsonMigrationObserver.Void
) =
    JsonMigration.migrate(this, jsonNode, MemoizedMeldingerSupplier(meldingerSupplier), observer)

private class MemoizedMeldingerSupplier(private val supplier: MeldingerSupplier): MeldingerSupplier {
    private val meldinger: Map<UUID, Pair<Navn, Json>> by lazy { supplier.hentMeldinger() }

    override fun hentMeldinger(): Map<UUID, Pair<Navn, Json>> = meldinger
}

interface JsonMigrationObserver {
    object Void : JsonMigrationObserver {
        override fun vedtaksperiodeSlettet(vedtaksperiodeId: UUID, vedtaksperiodeNode: JsonNode) {}
    }
    fun vedtaksperiodeSlettet(vedtaksperiodeId: UUID, vedtaksperiodeNode: JsonNode)
}

// Implements GoF Command Pattern to perform migration
internal abstract class JsonMigration(private val version: Int) {
    internal companion object {
        private val log = LoggerFactory.getLogger(JsonMigration::class.java)
        private const val SkjemaversjonKey = "skjemaVersjon"
        private const val InitialVersion = 0

        internal fun migrate(
            migrations: List<JsonMigration>,
            jsonNode: JsonNode,
            supplier: MeldingerSupplier,
            observer: JsonMigrationObserver
        ) = jsonNode.apply {
            require(this is ObjectNode) { "Kan kun migrere ObjectNodes" }
            val sortedMigrations = migrations.sortedBy { it.version }
            require(sortedMigrations.windowed(2).none { (a, b) -> a.version == b.version }) { "Versjoner må være unike" }
            sortedMigrations.forEach { it.migrate(this, supplier, observer) }
        }

        internal fun medSkjemaversjon(migrations: List<JsonMigration>, jsonNode: JsonNode): ObjectNode =
            (jsonNode as ObjectNode).put(SkjemaversjonKey, gjeldendeVersjon(migrations))

        internal fun skjemaVersjon(jsonNode: JsonNode) =
            jsonNode.path(SkjemaversjonKey).asInt(InitialVersion)

        internal fun gjeldendeVersjon(migrations: List<JsonMigration>) =
            migrations.maxOfOrNull { it.version } ?: InitialVersion
    }

    init {
        require(version > InitialVersion) { "Ugyldig versjon for migrering. Må være større enn $InitialVersion" }
    }

    protected abstract val description: String

    private fun migrate(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier, observer: JsonMigrationObserver) {
        if (!shouldMigrate(jsonNode)) return
        doMigration(jsonNode, meldingerSupplier, observer)
        after(jsonNode)
    }

    protected abstract fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier)
    protected open fun doMigration(
        jsonNode: ObjectNode,
        meldingerSupplier: MeldingerSupplier,
        observer: JsonMigrationObserver
    ) {
        doMigration(jsonNode, meldingerSupplier)
    }

    protected open fun shouldMigrate(jsonNode: JsonNode) =
        skjemaVersjon(jsonNode) < version

    private fun after(jsonNode: ObjectNode) {
        log.info("Successfully migrated json to $version using ${this.javaClass.name}: ${this.description}")
        jsonNode.put(SkjemaversjonKey, version)
    }
}
