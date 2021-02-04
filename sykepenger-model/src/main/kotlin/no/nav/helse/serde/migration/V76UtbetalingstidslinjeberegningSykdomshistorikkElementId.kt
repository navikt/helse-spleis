package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDateTime
import java.util.*

internal class V76UtbetalingstidslinjeberegningSykdomshistorikkElementId : JsonMigration(version = 76) {
    override val description: String = "Legger på sykdomshistorikkelementId på Utbetalingstidslinjeberegning"

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val sykdomshistorikkelementer = arbeidsgiver.path("sykdomshistorikk").map { element ->
                UUID.fromString(element.path("id").asText()) to LocalDateTime.parse(element.path("tidsstempel").asText())
            }.toMap()

            arbeidsgiver.path("beregnetUtbetalingstidslinjer").forEach { element ->
                migrateBeregnetUtbetalingstidslinje(element as ObjectNode, sykdomshistorikkelementer)
            }
        }
    }

    private fun migrateBeregnetUtbetalingstidslinje(element: ObjectNode, sykdomshistorikkelementer: Map<UUID, LocalDateTime>) {
        val tidsstempel = LocalDateTime.parse(element.path("tidsstempel").asText())
        element.put("sykdomshistorikkElementId", finnSykdomshistorikkElement(tidsstempel, sykdomshistorikkelementer).toString())
    }

    private fun finnSykdomshistorikkElement(tidsstempel: LocalDateTime, sykdomshistorikkelementer: Map<UUID, LocalDateTime>) =
        sykdomshistorikkelementer
            .filterValues { it <= tidsstempel }
            .maxByOrNull { (_, other) -> other }
            ?.key ?: throw IllegalStateException("fant ikke sykdomshistorikkElementId for beregning opprettet $tidsstempel")
}

