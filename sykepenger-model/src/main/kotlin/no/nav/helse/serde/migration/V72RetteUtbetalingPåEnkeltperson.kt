package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V72RetteUtbetalingPåEnkeltperson : JsonMigration(version = 72) {
    override val description: String = "Retter tidligere migreringsfeil for en spesifikk utbetaling"

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("utbetalinger").firstOrNull { utbetaling ->
                utbetaling.path("id").asText() == "231b36a9-0e2e-4c6a-a7c2-353f96c23552"
            }?.let {
                (it as ObjectNode).put("type", "UTBETALING")
            }
        }
    }
}
