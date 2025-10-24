package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V337FjernerRevurderingFeilet : JsonMigration(337) {
    override val description = "Fjerner REVURDERING_FEILET og bytter den ut med TIL_INFOTRYGD"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("forkastede").forEach { vedtaksperiode ->
                migrerVedtaksperiode(vedtaksperiode.path("vedtaksperiode"))
            }
        }
    }

    private fun migrerVedtaksperiode(vedtaksperiode: JsonNode) {
        if (vedtaksperiode.path("tilstand").asText() == "REVURDERING_FEILET") {
            (vedtaksperiode as ObjectNode).put("tilstand", "TIL_INFOTRYGD")
        }
    }
}
