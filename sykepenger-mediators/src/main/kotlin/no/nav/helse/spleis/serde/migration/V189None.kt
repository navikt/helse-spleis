package no.nav.helse.spleis.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V189None : JsonMigration(version = 189) {
    override val description = """[fjernet]"""

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {}
}