package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V223SpissetMigreringForÅForkasteUtbetaling: JsonMigration(223) {

    private companion object {
        private val trøblete = TrøbleteUtbetalinger(setOf(
            "55e6c959-d633-4b84-8b11-4292566b311a"
        ))
    }

    override val description = "spisset migrering for å forkaste trøblete utbetalinger"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        trøblete.doMigration(jsonNode)
    }
}
