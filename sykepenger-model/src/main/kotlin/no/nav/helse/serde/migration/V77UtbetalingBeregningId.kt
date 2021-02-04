package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDateTime
import java.util.*

internal class V77UtbetalingBeregningId : JsonMigration(version = 77) {
    override val description: String = "Legger på beregningId på Utbetaling"
    private val ukjentBeregningId = UUID.fromString("00000000-0000-0000-0000-000000000000")

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val beregninger = arbeidsgiver.path("beregnetUtbetalingstidslinjer").map { element ->
                UUID.fromString(element.path("id").asText()) to LocalDateTime.parse(element.path("tidsstempel").asText())
            }.toMap()

            arbeidsgiver.path("utbetalinger").forEach { element ->
                migrateUtbetaling(element as ObjectNode, beregninger)
            }
        }
    }

    private fun migrateUtbetaling(element: ObjectNode, beregninger: Map<UUID, LocalDateTime>) {
        val tidsstempel = LocalDateTime.parse(element.path("tidsstempel").asText())
        element.put("beregningId", finnBeregningId(tidsstempel, beregninger).toString())
    }

    private fun finnBeregningId(tidsstempel: LocalDateTime, beregninger: Map<UUID, LocalDateTime>) =
        beregninger
            .filterValues { it <= tidsstempel }
            .maxByOrNull { (_, other) -> other }
            ?.key ?: ukjentBeregningId
}

