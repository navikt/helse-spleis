package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDateTime

internal class V23SykdomshistorikkMerge : JsonMigration(version = 23) {
    override val description = "Legger inn en sykdomstidslinje i arbeidsgiver som er sammensatt fra vedtaksperioder"

    override fun doMigration(jsonNode: ObjectNode) {
        for (arbeidsgiver in jsonNode.path("arbeidsgivere")) {
            arbeidsgiver as ObjectNode
            val nullElementer =
                arbeidsgiver.path("forkastede").map { it.path("sykdomshistorikk").first().deepCopy<ObjectNode>() }
                    .onEach {
                        it.remove("hendelseId")
                        it.put("tidsstempel", LocalDateTime.parse(it["tidsstempel"].asText()).plusNanos(1).toString())
                        it.putArray("hendelseSykdomstidslinje")
                    }
            val originaleElementer =
                (elementer(arbeidsgiver, "forkastede") + elementer(arbeidsgiver, "vedtaksperioder") + nullElementer)
                    .sortedBy { LocalDateTime.parse(it["tidsstempel"].asText()) }
                    .map { it.deepCopy<ObjectNode>() }

            var elementer = (listOf(originaleElementer.first()) + originaleElementer
                .zipWithNext { nåværende, neste ->
                    neste.also { resultat ->
                        resultat["hendelseId"]?.let {
                            kombinerHendelse(nåværende, neste)
                            kombinerBeregnetSykdomstidslinje(nåværende, neste)
                        } ?: fjernDatoerFraNeste(nåværende, neste)
                    }
                })
            val kunNull = elementer.filter { it["hendelseId"] == null }
            elementer = elementer.filterNot { it["hendelseId"] == null }
                .reversed()
                .distinctBy { it["hendelseId"] }
                .plus(kunNull)
                .sortedByDescending { it["tidsstempel"].asText() }

            arbeidsgiver.putArray("sykdomshistorikk").addAll(elementer)
        }
    }

    private fun elementer(arbeidsgiver: JsonNode, path: String) = arbeidsgiver.path(path)
        .flatMap { it.path("sykdomshistorikk") }

    private fun kombinerBeregnetSykdomstidslinje(
        nåværende: JsonNode,
        neste: ObjectNode
    ) {
        val kombinert = kombinerTidslinje(
            nåværende["beregnetSykdomstidslinje"],
            neste["beregnetSykdomstidslinje"]
        )
        neste.putArray("beregnetSykdomstidslinje").addAll(kombinert)
    }

    private fun kombinerHendelse(nåværende: JsonNode, neste: ObjectNode) {
        if (nåværende["hendelseId"] == neste["hendelseId"]) {
            val hendelseKombinert = kombinerTidslinje(
                nåværende["hendelseSykdomstidslinje"],
                neste["hendelseSykdomstidslinje"]
            )
            neste.putArray("hendelseSykdomstidslinje").addAll(hendelseKombinert)
        }
    }

    private fun kombinerTidslinje(nåværende: JsonNode, neste: JsonNode): List<JsonNode> {
        val datoerFraNeste = neste.map { it["dato"].asText() }
        return (nåværende.filterNot { it["dato"].asText() in datoerFraNeste } + neste)
            .sortedBy { it["dato"].asText() }
    }

    private fun fjernDatoerFraNeste(nåværende: JsonNode, neste: ObjectNode) {
        val datoerFraNeste = neste["beregnetSykdomstidslinje"].map { it["dato"].asText() }
        neste.putArray("beregnetSykdomstidslinje")
            .addAll(nåværende["beregnetSykdomstidslinje"].filterNot { it["dato"].asText() in datoerFraNeste })
            .sortedBy { it["dato"].asText() }
    }
}
