package no.nav.helse.spleis.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V68FikseØdelagteUtbetalinger : JsonMigration(version = 68) {
    override val description: String = "Fikser ødelagt utbetaling"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        /* migrering gjort */
    }
}
