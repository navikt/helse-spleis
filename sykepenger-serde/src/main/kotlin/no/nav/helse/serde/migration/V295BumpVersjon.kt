package no.nav.helse.serde.migration

import tools.jackson.databind.node.ObjectNode

internal class V295BumpVersjon : JsonMigration(version = 295) {
    override val description = "bumper versjon"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {}
}
