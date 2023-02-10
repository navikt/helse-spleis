package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory

internal class V218SpissetMigreringForÅForkasteUtbetaling: JsonMigration(218) {

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
        private val trøbleteUtbetalinger = setOf(
            "42e15bdb-a176-4681-a17f-b897202b0042"
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