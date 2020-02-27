package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V7ForbrukteSykedager : JsonMigration(version = 7) {

    override val description = "Setter forbrukteSykedager til: null på perioder hvor vi ikke har data"

    private val forbrukteSykedagerKey = "forbrukteSykedager"

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { periode ->
                migrerVedtaksperiode(periode)
            }
        }
    }

    private fun migrerVedtaksperiode(periode: JsonNode) {
        if (periode.has(forbrukteSykedagerKey)) return
        (periode as ObjectNode).putNull(forbrukteSykedagerKey)
    }
}
