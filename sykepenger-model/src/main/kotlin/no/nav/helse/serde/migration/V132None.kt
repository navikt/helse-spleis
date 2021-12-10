package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V132None : JsonMigration(version = 132) {
    override val description = "[SPISSET MIGRERING UTFØRT]"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {}
}
