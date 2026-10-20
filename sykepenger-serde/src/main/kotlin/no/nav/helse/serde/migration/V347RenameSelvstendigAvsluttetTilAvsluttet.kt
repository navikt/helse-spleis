package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V347RenameSelvstendigAvsluttetTilAvsluttet : JsonMigration(347) {
    override val description = "Renamer tilstand SELVSTENDIG_AVSLUTTET til AVSLUTTET"

    override fun doMigration(
        jsonNode: ObjectNode,
        meldingerSupplier: MeldingerSupplier,
    ) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { vedtaksperiode ->
                migrerVedtaksperiode(vedtaksperiode)
            }
        }
    }

    private fun migrerVedtaksperiode(vedtaksperiode: JsonNode) {
        if (vedtaksperiode.path("tilstand").asText() == "SELVSTENDIG_AVSLUTTET") {
            (vedtaksperiode as ObjectNode).put("tilstand", "AVSLUTTET")
        }
    }
}
