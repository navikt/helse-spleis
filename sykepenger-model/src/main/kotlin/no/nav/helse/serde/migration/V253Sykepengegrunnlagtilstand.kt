package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V253Sykepengegrunnlagtilstand: JsonMigration(253) {

    override val description = "Migrere inn tilstand for sykepengegrunnlag"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("vilkårsgrunnlagHistorikk").forEach { innslag ->
            innslag.path("vilkårsgrunnlag").forEach { element ->
                val node = element.path("sykepengegrunnlag") as ObjectNode
                node.put("tilstand", "FASTSATT")
            }
        }
    }
}