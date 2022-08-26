package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V73MergeAvventerGapOgAvventerInntektsmeldingFerdigGap : JsonMigration(version = 73) {

    override val description: String = "Merger AVVENTER_GAP og AVVENTER_INNTEKTSMELDING_FERDIG_GAP"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").mergeTilstander()
            arbeidsgiver.path("forkastede").mergeTilstanderForkastede()
        }
    }

    private fun Iterable<JsonNode>.mergeTilstander() = this
        .filter { it.path("tilstand").textValue() in arrayListOf("AVVENTER_GAP", "AVVENTER_INNTEKTSMELDING_FERDIG_GAP") }
        .forEach { (it as ObjectNode).put("tilstand", "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP") }

    private fun Iterable<JsonNode>.mergeTilstanderForkastede() = this
        .map { it.path("vedtaksperiode") }.mergeTilstander()
}
