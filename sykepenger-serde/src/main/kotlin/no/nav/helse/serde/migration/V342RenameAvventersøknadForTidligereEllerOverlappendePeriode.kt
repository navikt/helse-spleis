package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V342RenameAvventersøknadForTidligereEllerOverlappendePeriode : JsonMigration(342) {
    override val description = "Renamer tilstand AVVENTER_SØKNAD_FOR_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODE til AVVENTER_SØKNAD_FOR_OVERLAPPENDE_PERIODE"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { vedtaksperiode ->
                migrerVedtaksperiode(vedtaksperiode)
            }
        }
    }

    private fun migrerVedtaksperiode(vedtaksperiode: JsonNode) {
        if (vedtaksperiode.path("tilstand").asText() == "AVVENTER_SØKNAD_FOR_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODE") {
            (vedtaksperiode as ObjectNode).put("tilstand", "AVVENTER_SØKNAD_FOR_OVERLAPPENDE_PERIODE")
        }
    }
}
