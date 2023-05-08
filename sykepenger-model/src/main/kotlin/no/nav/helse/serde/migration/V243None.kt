package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V243None : JsonMigration(version = 243) {
    override val description: String = "[utført]"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        // denne migreringen har kjørt og er ferdig
    }
}
