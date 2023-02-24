package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V231NyttTilstandsnavnAvventerInntektsmelding: JsonMigration(231) {

    override val description = "migrerer tilstand AvventerInntektmeldingEllerHistorikk til AvventerInntektsmelding"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach {
            it.path("vedtaksperioder").toList().endreTilstand()
            it.path("forkastede").map {
                it.path("vedtaksperiode")
            }.endreTilstand()
        }
    }

    private fun List<JsonNode>.endreTilstand() {
        filter { vedtaksperiode ->
            vedtaksperiode.path("tilstand").asText() == "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK"
        }.forEach { vedtaksperiode ->
            vedtaksperiode as ObjectNode
            vedtaksperiode.put("tilstand", "AVVENTER_INNTEKTSMELDING")
        }
    }
}
