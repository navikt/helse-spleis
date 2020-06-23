package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V23None : JsonMigration(version = 23) {
    override val description: String = "Den originale V23 ble oppdatert til V24. Dette er tom migrering."

    override fun doMigration(jsonNode: ObjectNode) {
        //Denne skal være tom. Migrering flyttet til V24
    }
}
