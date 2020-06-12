package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDateTime

internal class V21SykdomshistorikkMerge : JsonMigration(version = 21) {
    override val description = "Legger inn en sykdomstidslinje i arbeidsgiver som er sammensatt fra vedtaksperioder"

    override fun doMigration(jsonNode: ObjectNode) {
        for (arbeidsgiver in jsonNode.path("arbeidsgivere")) {
            arbeidsgiver as ObjectNode
            val originaleElementer = (elementer(arbeidsgiver, "forkastede") + elementer(arbeidsgiver, "vedtaksperioder"))
                .sortedBy { LocalDateTime.parse(it["tidsstempel"].asText()) }

            val elementer = (listOf(originaleElementer.first()) + originaleElementer
                .zipWithNext { nåværende, neste ->
                    neste.deepCopy<ObjectNode>().also { resultat ->
                        kombinerHendelse(nåværende, neste, resultat)
                        kombinerBeregnetSykdomstidslinje(nåværende, neste, resultat)
                    }
                })
                .reversed()
                .distinctBy { it["hendelseId"] }

            arbeidsgiver.withArray("sykdomshistorikk")
                .addAll(elementer)
        }
    }

    private fun elementer(arbeidsgiver: JsonNode, path: String) = arbeidsgiver.path(path)
        .flatMap { it.path("sykdomshistorikk") }

    private fun kombinerBeregnetSykdomstidslinje(
        nåværende: JsonNode,
        neste: JsonNode,
        resultat: ObjectNode
    ) {
        val kombinert = kombinerBeregnetTidslinje(
            nåværende["beregnetSykdomstidslinje"],
            neste["beregnetSykdomstidslinje"]
        )
        resultat.putArray("beregnetSykdomstidslinje")
            .addAll(kombinert)
    }

    private fun kombinerHendelse(nåværende: JsonNode, neste: JsonNode, resultat: ObjectNode) {
        if (nåværende["hendelseId"] == neste["hendelseId"]) {
            val hendelseKombinert = kombinerBeregnetTidslinje(
                nåværende["hendelseSykdomstidslinje"],
                neste["hendelseSykdomstidslinje"]
            )
            resultat.putArray("hendelseSykdomstidslinje")
                .addAll(hendelseKombinert)
        }
    }

    fun kombinerBeregnetTidslinje(nåværende: JsonNode, neste: JsonNode): List<JsonNode> {
        val datoerFraNeste = neste.map { it["dato"].asText() }
        return (nåværende.filterNot { it["dato"].asText() in datoerFraNeste } + neste).sortedBy { it["dato"].asText() }
    }
}
