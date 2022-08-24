package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.person.AktivitetsloggObserver

internal class V149ResetLåstePerioder : JsonMigration(version = 149) {
    override val description: String = "Erstattet av migrering V151"

    override fun doMigration(
        jsonNode: ObjectNode,
        meldingerSupplier: MeldingerSupplier,
        observer: AktivitetsloggObserver
    ) {
        // this page was intentionally left blank
    }
}
