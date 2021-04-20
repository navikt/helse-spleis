package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V4LeggTilNySykdomstidslinje : JsonMigration(version = 4) {
    // Denne migreringen er ikke relevant lenger. Gjeldende migrering ligger i V6.
    override val description = "Utdatert, ingen migrering"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {}
}
