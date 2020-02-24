package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.serde.migration.V2EndreTilstandTyper.JsonTilstandTypeGammel.AVVENTER_SENDT_SØKNAD
import no.nav.helse.serde.migration.V2EndreTilstandTyper.JsonTilstandTypeGammel.MOTTATT_NY_SØKNAD
import no.nav.helse.serde.migration.V2EndreTilstandTyper.JsonTilstandTypeNy.AVVENTER_SØKNAD
import no.nav.helse.serde.migration.V2EndreTilstandTyper.JsonTilstandTypeNy.MOTTATT_SYKMELDING

internal class V2EndreTilstandTyper : JsonMigration(version = 2) {

    override val description = "Endrer TilstandType-enumer fordi VedtaksperiodeTilstand-objektene har fått nye navn"

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

        (periode as ObjectNode).put(
            tilstandKey, when (JsonTilstandTypeGammel.valueOf(tilstand)) {
            MOTTATT_NY_SØKNAD -> MOTTATT_SYKMELDING
            AVVENTER_SENDT_SØKNAD -> AVVENTER_SØKNAD
        }.name)
    }

    private enum class JsonTilstandTypeGammel {
        MOTTATT_NY_SØKNAD,
        AVVENTER_SENDT_SØKNAD
    }

    private enum class JsonTilstandTypeNy {
        MOTTATT_SYKMELDING,
        AVVENTER_SØKNAD
    }
}
