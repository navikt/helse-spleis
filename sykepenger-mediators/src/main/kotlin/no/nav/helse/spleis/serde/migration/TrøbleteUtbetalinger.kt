package no.nav.helse.spleis.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class TrøbleteUtbetalinger(private val trøbleteUtbetalinger: Set<String>) {
    internal fun doMigration(jsonNode: ObjectNode) {
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