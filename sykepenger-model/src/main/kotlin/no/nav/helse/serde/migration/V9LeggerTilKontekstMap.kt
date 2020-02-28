package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V9LeggerTilKontekstMap : JsonMigration(version = 9) {

    override val description = "Legger til tom kontekstMap til eksisterende aktiviteter"

    private val aktivitetsloggKey = "aktivitetslogg"

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode.path(aktivitetsloggKey).path("aktiviteter").forEach { aktivitet ->
            aktivitet.path("kontekster").forEach { kontekst ->
                (kontekst as ObjectNode).remove("melding")
                kontekst.with("kontekstMap")
            }
        }
    }
}
