package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V37None : JsonMigration(version = 37) {
    override val description: String = "Den originale V37 ble oppdatert til V38. Dette er en tom migrering."

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        //Denne skal være tom. Migrering flyttet til V38
    }
}
