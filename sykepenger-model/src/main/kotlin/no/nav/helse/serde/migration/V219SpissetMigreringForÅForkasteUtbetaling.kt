package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory

internal class V219SpissetMigreringForÅForkasteUtbetaling: JsonMigration(219) {

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
        private val trøbleteUtbetalinger = setOf(
            "acd8034b-35ee-4777-a22b-d891217cc490"
        )
    }

    override val description = "spisset migrering for å forkaste trøblete utbetalinger"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("utbetalinger")
                .filter { utbetaling ->
                    utbetaling.path("id").asText() in trøbleteUtbetalinger
                }
                .forEach { utbetaling ->
                    (utbetaling as ObjectNode).put("status", "FORKASTET")
                }
        }
    }
}