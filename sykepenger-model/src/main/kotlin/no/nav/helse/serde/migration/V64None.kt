package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V64None : JsonMigration(version = 64) {
    override val description: String = "Endrer status og type på automatisk genererte utbetalte annulleringer"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        //Denne skal være tom. Migrering flyttet til V67
    }
}
