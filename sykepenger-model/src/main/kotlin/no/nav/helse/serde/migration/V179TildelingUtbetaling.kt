package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V179TildelingUtbetaling: JsonMigration(179) {
    private companion object {
        private val utbetalinger = mapOf(
            "32e6e6a9-6d23-4046-80a1-9030cd8f5a58" to setOf(
                "b8f6ae4c-63e8-4575-ae36-932554ac2577"
            )
        )
    }
    override val description = "Fikse tildeling av utbetaling"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { vedtaksperiode ->
                val vedtaksperiodeId = vedtaksperiode.path("id").asText()
                val utbetalingId = utbetalinger.entries.firstOrNull { (_, vedtaksperioder) -> vedtaksperiodeId in vedtaksperioder }
                if (utbetalingId != null) (vedtaksperiode.path("utbetalinger") as ArrayNode).add(utbetalingId.key)
            }
        }
    }
}