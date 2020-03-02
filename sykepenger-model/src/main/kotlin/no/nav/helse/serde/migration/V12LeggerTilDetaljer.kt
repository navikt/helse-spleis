package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V12LeggerTilDetaljer : JsonMigration(version = 12) {

    override val description = "Legger til detaljer på eksisterende aktiviteter"

    private val aktivitetsloggKey = "aktivitetslogg"
    private val aktiviteterKey = "aktiviteter"
    private val detaljerKey = "detaljer"

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode.path(aktivitetsloggKey).path(aktiviteterKey).forEach { aktivitet ->
            (aktivitet as ObjectNode).with(detaljerKey)
        }
    }
}
