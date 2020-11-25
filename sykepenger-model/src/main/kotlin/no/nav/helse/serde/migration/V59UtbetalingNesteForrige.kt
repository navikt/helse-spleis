package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V59UtbetalingNesteForrige : JsonMigration(version = 59) {
    override val description: String = "[tom migrering]"

    override fun doMigration(jsonNode: ObjectNode) { /* reverted */ }
}

