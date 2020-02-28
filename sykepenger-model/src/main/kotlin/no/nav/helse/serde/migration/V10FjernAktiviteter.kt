package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V10FjernAktiviteter : JsonMigration(version = 10) {

    override val description = "Fjerner alle aktiviteter"

    private val aktivitetsloggKey = "aktivitetslogg"

    override fun doMigration(jsonNode: ObjectNode) {
        (jsonNode.path(aktivitetsloggKey) as ObjectNode).putArray("aktiviteter")
    }
}
