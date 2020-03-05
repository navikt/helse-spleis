package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V14EndreUtbetaltTilstandType : JsonMigration(version = 14) {

    override val description = "Endrer TilstandType fra Utbetalt til Avsluttet"

    private val tilstandKey = "tilstand"

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { periode ->
                migrerTilstandtype(periode)
            }
        }
    }

    private fun migrerTilstandtype(periode: JsonNode) {
        val tilstand = periode[tilstandKey].asText()
        if (tilstand !in JsonTilstandTypeGammel.values().map(Enum<*>::name)) return
        (periode as ObjectNode).put(tilstandKey, JsonTilstandTypeGammel.valueOf(tilstand).erstatning.name)
    }

    private enum class JsonTilstandTypeGammel(val erstatning: JsonTilstandTypeNy) {
        UTBETALT(JsonTilstandTypeNy.AVSLUTTET)
    }

    private enum class JsonTilstandTypeNy {
        AVSLUTTET
    }
}
