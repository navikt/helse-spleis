package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V255SpissetMigreringForÅForkasteUtbetaling: JsonMigration(255) {

    private companion object {
        private val trøblete = TrøbleteUtbetalinger(setOf(
            "5d91f5e4-4cf8-4461-84ec-6aa4fb955612"
        ))
    }

    override val description = "spisset migrering for å forkaste trøblete utbetalinger"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        trøblete.doMigration(jsonNode)
    }
}
