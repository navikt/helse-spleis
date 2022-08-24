package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.person.AktivitetsloggObserver

internal class V87InfotrygdhistorikkStatslønn : JsonMigration(version = 87) {
    override val description: String = "Infotrygdhistorikk med statslønn"

    override fun doMigration(
        jsonNode: ObjectNode,
        meldingerSupplier: MeldingerSupplier,
        observer: AktivitetsloggObserver
    ) {
        jsonNode.path("infotrygdhistorikk").forEach {
            (it as ObjectNode).put("harStatslønn", false)
        }
    }
}
