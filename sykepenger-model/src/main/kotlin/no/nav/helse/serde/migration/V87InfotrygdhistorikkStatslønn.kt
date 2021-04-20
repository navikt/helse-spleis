package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V87InfotrygdhistorikkStatslønn : JsonMigration(version = 87) {
    override val description: String = "Infotrygdhistorikk med statslønn"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("infotrygdhistorikk").forEach {
            (it as ObjectNode).put("harStatslønn", false)
        }
    }
}
