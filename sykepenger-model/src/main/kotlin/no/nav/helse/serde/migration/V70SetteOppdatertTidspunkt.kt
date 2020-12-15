package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDateTime

internal class V70SetteOppdatertTidspunkt : JsonMigration(version = 70) {
    override val description: String = "Sette oppdatert-tidspunkt for Utbetaling"

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("utbetalinger").forEach { utbetaling ->
                val opprettet = LocalDateTime.parse(utbetaling.path("tidsstempel").asText())
                val største = listOfNotNull(
                    opprettet,
                    utbetaling.path("vurdering").path("tidspunkt").kanskjedatotid(),
                    utbetaling.path("overføringstidspunkt").kanskjedatotid(),
                    utbetaling.path("avsluttet").kanskjedatotid()
                ).maxOrNull() ?: opprettet
                utbetaling as ObjectNode
                utbetaling.put("oppdatert", "$største")
            }
        }
    }

    private fun JsonNode.kanskjedatotid() = takeIf(JsonNode::isTextual)?.asText()?.let { LocalDateTime.parse(it) }
}
