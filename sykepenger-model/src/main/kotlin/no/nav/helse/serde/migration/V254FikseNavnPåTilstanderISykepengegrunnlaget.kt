package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V254FikseNavnPåTilstanderISykepengegrunnlaget: JsonMigration(254) {

    override val description = "Fikse navn på tilstander i sykepengegrunnlaget"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("vilkårsgrunnlagHistorikk").forEach { innslag ->
            innslag.path("vilkårsgrunnlag").forEach { element ->
                val node = element.path("sykepengegrunnlag") as ObjectNode
                val tilstand = node.path("tilstand").asText()
                if (tilstand in setOf("FASTSATT", "AVVENTER_FASTSETTELSE_ETTER_HOVEDREGEL")) node.put("tilstand", "FASTSATT_ETTER_HOVEDREGEL")
            }
        }
    }
}