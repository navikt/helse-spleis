package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V39SetterAutomatiskBehandlingPåVedtaksperiode : JsonMigration(version = 39) {
    override val description: String = "Setter automatiskBehandling = false på alle godkjente vedtaksperioder"

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode["arbeidsgivere"].forEach { arbeidsgiver ->
            kopierHendelseIderFraHistorikk(arbeidsgiver["vedtaksperioder"])
            kopierHendelseIderFraHistorikk(arbeidsgiver["forkastede"])
        }
    }

    private fun kopierHendelseIderFraHistorikk(perioder: JsonNode) {
        perioder.forEach { periode ->
            periode as ObjectNode
            if(periode.path("godkjentAv").let {it.isNull || it.isMissingNode}) {
                periode.putNull("automatiskBehandling")
            } else {
                periode.put("automatiskBehandling", false)
            }
        }
    }

}
