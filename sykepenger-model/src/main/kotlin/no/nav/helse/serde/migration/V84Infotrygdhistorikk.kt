package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.person.AktivitetsloggObserver

internal class V84Infotrygdhistorikk : JsonMigration(version = 84) {
    override val description: String = "Infotrygdhistorikk"

    override fun doMigration(
        jsonNode: ObjectNode,
        meldingerSupplier: MeldingerSupplier,
        observer: AktivitetsloggObserver
    ) {
        jsonNode.putArray("infotrygdhistorikk")
    }
}
