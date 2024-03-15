package no.nav.helse.spleis.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V156None: JsonMigration(version = 156) {
    override val description = "Erstattes av mer treffsikker dry run V157"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {}
}
