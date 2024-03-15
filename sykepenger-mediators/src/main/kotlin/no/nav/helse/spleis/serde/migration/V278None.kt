package no.nav.helse.spleis.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V278None : JsonMigration(version = 278) {
    override val description = "[none]"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {}
}