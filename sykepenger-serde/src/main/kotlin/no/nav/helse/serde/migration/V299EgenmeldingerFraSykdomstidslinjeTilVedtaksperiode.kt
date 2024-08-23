package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.serde.serdeObjectMapper

internal class V299EgenmeldingerFraSykdomstidslinjeTilVedtaksperiode: JsonMigration(version = 299) {
    override val description = "lagrer egenmeldingsdager på vedtaksperiode"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { periode ->
                migrerVedtaksperiode(periode)
            }
        }
    }

    private fun migrerVedtaksperiode(vedtaksperiode: JsonNode) {
        val egenmeldingsperioderFraSykdomstidslinjen = vedtaksperiode["behandlinger"]
            .lastOrNull()
            ?.get("endringer")
            ?.lastOrNull()
            ?.get("sykdomstidslinje")
            ?.get("dager")
            ?.filter { dag -> dag["type"].asText() == "ARBEIDSGIVERDAG" && dag["kilde"]["type"].asText() == "Søknad" }
            ?.map { dag ->
                if (dag["dato"].isNull) {
                    serdeObjectMapper.createObjectNode()
                        .put("fom", dag["fom"].asText())
                        .put("tom", dag["tom"].asText())
                }
                else {
                    serdeObjectMapper.createObjectNode()
                        .put("fom", dag["dato"].asText())
                        .put("tom", dag["dato"].asText())
                }
            }
            ?: emptyList()

        vedtaksperiode as ObjectNode
        val egenmeldingsperioder = vedtaksperiode.path("egenmeldingsperioder") as ArrayNode
        egenmeldingsperioder.removeAll()
        egenmeldingsperioder.addAll(egenmeldingsperioderFraSykdomstidslinjen)



    }
}