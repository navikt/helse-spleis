package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V333ArbeidsgiverperioderPåYrkesaktivitet : JsonMigration(333) {
    override val description = "Lager tom liste av arbeidsgiverperiode på yrkesaktivitet"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            (arbeidsgiver as ObjectNode).withArray("arbeidsgiverperioder")
        }
    }
}
