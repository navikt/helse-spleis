package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V224SpissetMigreringForÅForkasteUtbetaling: JsonMigration(224) {

    private companion object {
        private val trøblete = TrøbleteUtbetalinger(setOf(
            "1a72205b-4edb-44d9-b2de-4095c9d2ae45"
        ))
    }

    override val description = "spisset migrering for å forkaste trøblete utbetalinger"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        trøblete.doMigration(jsonNode)
    }
}
