package no.nav.helse.serde.migration

import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.ObjectNode

internal class V335PerioderUtenNavAnsvar : JsonMigration(335) {
    override val description = "Renamer arbeidsgiverperioder til perioderUtenNavAnsvar"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            (arbeidsgiver as ObjectNode).set("perioderUtenNavAnsvar", arbeidsgiver.path("arbeidsgiverperioder"))
        }
    }
}
