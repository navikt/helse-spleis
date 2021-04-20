package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V84Infotrygdhistorikk : JsonMigration(version = 84) {
    override val description: String = "Infotrygdhistorikk"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.putArray("infotrygdhistorikk")
    }
}
