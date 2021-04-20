package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V78VedtaksperiodeListeOverUtbetalinger : JsonMigration(version = 78) {
    override val description: String = "Lager liste over utbetalinger på Vedtaksperiode"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { vedtaksperiode ->
                migrateVedtaksperiode(vedtaksperiode as ObjectNode)
            }
            arbeidsgiver.path("forkastede").forEach { forkastet ->
                migrateVedtaksperiode(forkastet.path("vedtaksperiode") as ObjectNode)
            }
        }
    }

    private fun migrateVedtaksperiode(element: ObjectNode) {
        element.putArray("utbetalinger")
        if (element.hasNonNull("utbetalingId"))
            (element.path("utbetalinger") as ArrayNode).add(element.path("utbetalingId").asText())
    }
}

