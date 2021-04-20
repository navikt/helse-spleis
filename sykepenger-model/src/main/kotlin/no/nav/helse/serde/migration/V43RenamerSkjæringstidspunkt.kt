package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V43RenamerSkjæringstidspunkt : JsonMigration(version = 43) {
    override val description: String = "beregningsdatoFraInfotrygd renames til skjæringstidspunktFraInfotrygd"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            renameSkjæringstidspunkt(arbeidsgiver.path("vedtaksperioder"))
            renameSkjæringstidspunktForkastede(arbeidsgiver.path("forkastede"))
        }
    }

    private fun renameSkjæringstidspunkt(perioder: JsonNode) {
        perioder.forEach(::migrer)
    }

    private fun renameSkjæringstidspunktForkastede(perioder: JsonNode) {
        perioder.forEach { periode ->
            migrer(periode.path("vedtaksperiode"))
        }
    }

    private fun migrer(periode: JsonNode) {
        periode as ObjectNode
        periode.path("beregningsdatoFraInfotrygd")
            .takeIf(JsonNode::isTextual)
            ?.also { periode.put("skjæringstidspunktFraInfotrygd", it.textValue()) }
            ?: periode.putNull("skjæringstidspunktFraInfotrygd")
    }

}
