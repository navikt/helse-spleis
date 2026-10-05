package no.nav.helse.serde.migration

import tools.jackson.databind.node.ObjectNode

internal class V316BumpVersjon : JsonMigration(version = 316) {
    override val description = "bumper versjon"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {}
}
