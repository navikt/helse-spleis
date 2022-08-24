package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.person.AktivitetsloggObserver

internal class V68FikseØdelagteUtbetalinger : JsonMigration(version = 68) {
    override val description: String = "Fikser ødelagt utbetaling"

    override fun doMigration(
        jsonNode: ObjectNode,
        meldingerSupplier: MeldingerSupplier,
        observer: AktivitetsloggObserver
    ) {
        /* migrering gjort */
    }
}
