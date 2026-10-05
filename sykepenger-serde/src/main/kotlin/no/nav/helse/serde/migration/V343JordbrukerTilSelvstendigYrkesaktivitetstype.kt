package no.nav.helse.serde.migration

import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode

internal class V343JordbrukerTilSelvstendigYrkesaktivitetstype : JsonMigration(343) {
    override val description = "Endrer yrkesaktivitetstypen JORDBRUKER til SELVSTENDIG"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            migrerArbeidsgiver(arbeidsgiver)
        }
    }

    private fun migrerArbeidsgiver(arbeidsgiver: JsonNode) {
        if (arbeidsgiver.path("yrkesaktivitetstype").asText() == "JORDBRUKER") {
            (arbeidsgiver as ObjectNode).put("yrkesaktivitetstype", "SELVSTENDIG")
            arbeidsgiver.put("organisasjonsnummer", "SELVSTENDIG")
        }
    }
}
