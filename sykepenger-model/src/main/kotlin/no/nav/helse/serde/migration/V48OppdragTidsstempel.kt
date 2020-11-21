package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V48OppdragTidsstempel : JsonMigration(version = 48) {
    override val description: String = "Setter tidsstempel på når oppdraget ble laget"

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("utbetalinger").forEach { utbetaling ->
                val tidsstempel = utbetaling.path("tidsstempel").asText()
                utbetaling.settTidsstempel("arbeidsgiverOppdrag", tidsstempel)
                utbetaling.settTidsstempel("personOppdrag", tidsstempel)
            }
        }
    }

    private fun JsonNode.settTidsstempel(key: String, tidsstempel: String) {
        (path(key) as ObjectNode).put("tidsstempel", tidsstempel)
    }
}

