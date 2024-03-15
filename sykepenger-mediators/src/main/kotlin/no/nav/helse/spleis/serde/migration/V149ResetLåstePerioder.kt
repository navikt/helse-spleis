package no.nav.helse.spleis.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V149ResetLåstePerioder : JsonMigration(version = 149) {
    override val description: String = "Erstattet av migrering V151"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        // this page was intentionally left blank
    }
}
