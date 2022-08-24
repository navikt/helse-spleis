package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.person.AktivitetsloggObserver

internal class V50None : JsonMigration(version = 50) {
    override val description: String = "Den originale V50 ble oppdatert til V52. Dette er en tom migrering."

    override fun doMigration(
        jsonNode: ObjectNode,
        meldingerSupplier: MeldingerSupplier,
        observer: AktivitetsloggObserver
    ) {
        //Denne skal være tom. Migrering flyttet til V52
    }
}
