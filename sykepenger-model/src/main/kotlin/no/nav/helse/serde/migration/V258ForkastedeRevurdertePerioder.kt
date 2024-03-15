package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V258ForkastedeRevurdertePerioder : JsonMigration(version = 258) {
    private companion object {
        private val migrere = setOf(
            "AVVENTER_VILKÅRSPRØVING_REVURDERING",
            "AVVENTER_HISTORIKK_REVURDERING",
            "AVVENTER_SIMULERING_REVURDERING",
            "AVVENTER_GJENNOMFØRT_REVURDERING",
            "AVVENTER_GODKJENNING_REVURDERING",
            "AVVENTER_REVURDERING"
        )
    }
    override val description = "migrerer forkastede perioder til infotrygd"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("forkastede").forEach { forkastede ->
                val vedtaksperiode = forkastede.path("vedtaksperiode") as ObjectNode
                val tilstand = vedtaksperiode.path("tilstand").asText()
                if (tilstand in migrere) vedtaksperiode.put("tilstand", "TIL_INFOTRYGD")
            }
        }
    }
}