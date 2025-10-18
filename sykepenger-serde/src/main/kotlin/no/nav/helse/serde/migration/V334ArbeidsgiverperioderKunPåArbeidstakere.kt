package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V334ArbeidsgiverperioderKunPåArbeidstakere : JsonMigration(334) {
    override val description = "Lager tom liste av arbeidsgiverperiode på yrkesaktivitet"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            if (arbeidsgiver.path("yrkesaktivitetstype").asText() != "ARBEIDSTAKER") {
                (arbeidsgiver as ObjectNode).putArray("arbeidsgiverperioder")
                arbeidsgiver.path("vedtaksperioder").forEach { migrerVedtaksperiode(it) }
                arbeidsgiver.path("forkastede").forEach { migrerVedtaksperiode(it.path("vedtaksperiode")) }
            }
        }
    }

    private fun migrerVedtaksperiode(vedtaksperiode: JsonNode) {
        vedtaksperiode.path("behandlinger").forEach { behandling ->
            behandling.path("endringer").forEach { endring ->
                (endring.path("arbeidsgiverperiode") as ObjectNode).apply {
                    put("ferdigAvklart", false)
                    putArray("dager")
                }
            }
        }
    }
}
