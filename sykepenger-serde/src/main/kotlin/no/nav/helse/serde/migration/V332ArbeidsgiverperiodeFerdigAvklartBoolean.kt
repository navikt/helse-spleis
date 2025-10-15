package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V332ArbeidsgiverperiodeFerdigAvklartBoolean : JsonMigration(332) {
    override val description = "Lager tom liste av skjæringstidspunkter på person"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.putArray("skjæringstidspunkter")
    }
}
