package no.nav.helse.serde.migration

import tools.jackson.databind.node.ObjectNode

internal class V319BumpVersjon : JsonMigration(version = 319) {
    override val description = "bumper versjon"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {}
}
