package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V4LagerAktivitetslogg : JsonMigration(version = 4) {

    override val description = "Legger til tom aktivitetslogg i person"

    private val aktivitetsloggKey = "aktivitetslogg"
    private val aktiviteterKey = "aktiviteter"

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode.set<ObjectNode>(aktivitetsloggKey, ObjectMapper().valueToTree(aktiviteter()))
    }

    private fun aktiviteter() = mutableMapOf(
        aktiviteterKey to mutableListOf<Map<String, Any>>()
    )
}
