package no.nav.helse.serde.migration

import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.ObjectNode

internal class V294RenameTilBehandlinger : JsonMigration(version = 294) {
    override val description = "renamer generasjoner til behandlinger i json"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { migrerVedtaksperiode(it) }
            arbeidsgiver.path("forkastede").forEach { migrerVedtaksperiode(it.path("vedtaksperiode")) }
        }
    }

    private fun migrerVedtaksperiode(node: JsonNode) {
        val generasjoner = node.path("generasjoner").deepCopy()
        node as ObjectNode
        node.set("behandlinger", generasjoner)
        node.remove("generasjoner")
    }
}
