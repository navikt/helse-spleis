package no.nav.helse.spleis.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import java.util.UUID

internal class V261ForkastegamleUtbetalinger : JsonMigration(version = 261) {
    override val description = "forkaster utbetalinger som skulle vært forkastet"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val utbetalinger = arbeidsgiver.path("utbetalinger")
                .filter { it.path("status").asText() == "IKKE_UTBETALT" }
                .map { it as ObjectNode }
                .associateBy { UUID.fromString(it.path("id").asText()) }

            arbeidsgiver.path("vedtaksperioder")
                .filter { it.path("tilstand").asText() == "AVSLUTTET" }
                .forEach { periode ->
                    periode.path("utbetalinger").forEach { generasjon ->
                        val utbetalingId = UUID.fromString(generasjon.path("utbetalingId").asText())

                        if (utbetalingId in utbetalinger) {
                            utbetalinger.getValue(utbetalingId).put("status", "FORKASTET")
                        }
                    }
                }
        }
    }
}