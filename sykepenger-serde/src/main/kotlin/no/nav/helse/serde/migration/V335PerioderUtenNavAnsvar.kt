package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V335PerioderUtenNavAnsvar : JsonMigration(335) {
    override val description = "Renamer arbeidsgiverperioder til perioderUtenNavAnsvar"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            (arbeidsgiver as ObjectNode).set<ArrayNode>("perioderUtenNavAnsvar", arbeidsgiver.path("arbeidsgiverperioder"))
        }
    }
}
