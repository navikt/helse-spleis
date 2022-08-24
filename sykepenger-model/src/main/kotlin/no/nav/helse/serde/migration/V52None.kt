package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.person.AktivitetsloggObserver

internal class V52None : JsonMigration(version = 52) {
    override val description: String = "Den originale V52 ble oppdatert til V53. Dette er en tom migrering."

    override fun doMigration(
        jsonNode: ObjectNode,
        meldingerSupplier: MeldingerSupplier,
        observer: AktivitetsloggObserver
    ) {
        //Denne skal være tom. Migrering flyttet til V53
    }
}
