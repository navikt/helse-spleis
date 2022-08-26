package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDateTime

internal class V57UtbetalingAvsluttet : JsonMigration(version = 57) {
    override val description: String = "Utvider Utbetaling med avsluttet-tidspunkt"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode
            .path("arbeidsgivere")
            .forEach { arbeidsgiver ->
                arbeidsgiver
                    .path("utbetalinger")
                    .filter { it.hasNonNull("vurdering") }
                    .filter { it.path("status").asText() != "IKKE_GODKJENT" }
                    .map { it as ObjectNode }
                    .onEach {
                        val vurderingstidspunkt = it.path("vurdering").path("tidspunkt").let { LocalDateTime.parse(it.asText()) }
                        val overføringstidspunkt = it.path("overføringstidspunkt").takeIf(JsonNode::isTextual)?.let { LocalDateTime.parse(it.asText()) }
                        val avsluttettidsunkt = overføringstidspunkt ?: vurderingstidspunkt
                        it.put("avsluttet", "$avsluttettidsunkt")
                        val status = it.path("status").asText()
                        if (status == "GODKJENT" || status == "IKKE_UTBETALT") it.put("status", "GODKJENT_UTEN_UTBETALING")

                    }
            }
    }
}

