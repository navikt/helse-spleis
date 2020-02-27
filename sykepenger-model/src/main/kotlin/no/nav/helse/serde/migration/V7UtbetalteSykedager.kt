package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V7UtbetalteSykedager : JsonMigration(version = 7) {

    override val description = "Setter betalteSykedager til: -1 på perioder hvor vi ikke har data"

    private val betalteSykedagerKey = "betalteSykedager"

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { periode ->
                migrerVedtaksperiode(periode)
            }
        }
    }

    private fun migrerVedtaksperiode(periode: JsonNode) {
        if (periode.hasNonNull(betalteSykedagerKey)) return
        (periode as ObjectNode).put(betalteSykedagerKey, -1)
    }
}
