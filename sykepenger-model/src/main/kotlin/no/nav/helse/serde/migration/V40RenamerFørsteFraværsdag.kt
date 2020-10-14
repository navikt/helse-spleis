package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V40RenamerFørsteFraværsdag : JsonMigration(version = 40) {
    override val description: String = "førsteFraværsdag renames til beregningsdato"

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            renameFørsteFraværsdag(arbeidsgiver.path("vedtaksperioder"))
            renameFørsteFraværsdag(arbeidsgiver.path("forkastede"))
        }
    }

    private fun renameFørsteFraværsdag(perioder: JsonNode) {
        perioder.forEach { periode ->
            periode as ObjectNode
            periode.path("førsteFraværsdag")
                .takeIf(JsonNode::isTextual)
                ?.also { periode.put("beregningsdato", it.textValue()) }
                ?: periode.putNull("beregningsdato")
        }
    }

}
