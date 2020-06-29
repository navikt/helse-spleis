package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V24None : JsonMigration(version = 24) {
    override val description: String = "Den originale V24 ble oppdatert til V24. Dette er en tom migrering."

    override fun doMigration(jsonNode: ObjectNode) {
        //Denne skal være tom. Migrering flyttet til V26
    }
}
