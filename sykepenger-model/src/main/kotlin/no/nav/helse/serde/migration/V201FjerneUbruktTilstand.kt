package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V201FjerneUbruktTilstand : JsonMigration(version = 201) {
    override val description = """fjerner AVVENTER_ARBEIDSGIVERE_REVURDERING"""

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("forkastede").forEach { forkastet ->
                val vedtaksperiode = forkastet.path("vedtaksperiode") as ObjectNode
                if (vedtaksperiode.path("tilstand").asText() == tilstand) {
                    vedtaksperiode.put("tilstand", tilInfotrygd)
                }
            }
        }
    }

    private companion object {
        private const val tilstand = "AVVENTER_ARBEIDSGIVERE_REVURDERING"
        private const val tilInfotrygd = "TIL_INFOTRYGD"
    }
}