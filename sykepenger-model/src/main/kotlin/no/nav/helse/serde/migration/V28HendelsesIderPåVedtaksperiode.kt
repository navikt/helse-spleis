package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V28HendelsesIderPåVedtaksperiode : JsonMigration(version = 28) {
    override val description: String = "Kopierer hendelsesider fra sykdomshistorikk til vedtaksperioden."

    override fun doMigration(jsonNode: ObjectNode) {
        for (arbeidsgiver in jsonNode["arbeidsgivere"]) {
            kopierHendelseIderFraHistorikk(arbeidsgiver["vedtaksperioder"])
            kopierHendelseIderFraHistorikk(arbeidsgiver["forkastede"])
        }
    }

    private fun kopierHendelseIderFraHistorikk(perioder: JsonNode) {
        for (periode in perioder) {
            periode as ObjectNode
            periode.putArray("hendelseIder").addAll(periode["sykdomshistorikk"].map { it["hendelseId"] }.reversed())
        }
    }
}
