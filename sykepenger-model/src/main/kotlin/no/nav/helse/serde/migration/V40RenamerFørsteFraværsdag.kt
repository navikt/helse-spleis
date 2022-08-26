package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V40RenamerFørsteFraværsdag : JsonMigration(version = 40) {
    override val description: String = "førsteFraværsdag renames til beregningsdato"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            vedtaksperioder(arbeidsgiver.path("vedtaksperioder"))
            forkastede(arbeidsgiver.path("forkastede"))
        }
    }

    private fun vedtaksperioder(perioder: JsonNode) {
        perioder.forEach(::migrer)
    }

    private fun forkastede(perioder: JsonNode) {
        perioder.forEach { migrer(it.path("vedtaksperiode")) }
    }

    private fun migrer(periode: JsonNode) {
        periode as ObjectNode
        periode.path("førsteFraværsdag")
            .takeIf(JsonNode::isTextual)
            ?.also { periode.put("beregningsdato", it.textValue()) }
            ?: periode.putNull("beregningsdato")
    }

}
