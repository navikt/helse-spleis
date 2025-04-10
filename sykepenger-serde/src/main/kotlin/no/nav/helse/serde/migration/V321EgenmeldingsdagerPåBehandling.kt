package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.serde.serdeObjectMapper

internal class V321EgenmeldingsdagerPåBehandling: JsonMigration(version = 321) {
    override val description = "Legger til egenmeldingsdager på siste endring på siste behandling"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { periode ->
                migrerVedtaksperiode(periode)
            }
            arbeidsgiver.path("forkastede").forEach { forkastet ->
                migrerVedtaksperiode(forkastet.path("vedtaksperiode"))
            }
        }
    }

    private fun migrerVedtaksperiode(vedtaksperiode: JsonNode) {
        vedtaksperiode as ObjectNode
        val egenmeldingsperioder = vedtaksperiode.path("egenmeldingsperioder").takeIf(JsonNode::isArray)?.deepCopy<ArrayNode>() ?: serdeObjectMapper.createArrayNode()
        vedtaksperiode.path("behandlinger").forEach { behandling ->
            behandling.path("endringer").forEach { endring ->
                (endring as ObjectNode).putArray("egenmeldingsdager")
            }
        }
        (vedtaksperiode.path("behandlinger").last().path("endringer").last().path("egenmeldingsdager") as ArrayNode).apply {
            addAll(egenmeldingsperioder)
        }
    }
}
