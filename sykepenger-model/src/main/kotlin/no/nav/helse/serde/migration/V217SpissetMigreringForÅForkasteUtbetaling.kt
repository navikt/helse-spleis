package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory

internal class V217SpissetMigreringForÅForkasteUtbetaling: JsonMigration(217) {

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
        private val trøbleteUtbetalinger = setOf(
            "bd67d3ac-3a80-4681-b325-3f6d3045139d",
            "df357794-c74d-4644-aa91-bb1ee7c94bfb"
        )
    }

    private val trøblete = TrøbleteUtbetalinger(trøbleteUtbetalinger)
    override val description = "spisset migrering for å forkaste trøblete utbetalinger"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        trøblete.doMigration(jsonNode)
    }
}