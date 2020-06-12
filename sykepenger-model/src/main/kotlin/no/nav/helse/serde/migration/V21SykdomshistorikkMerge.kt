package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDateTime

internal class V21SykdomshistorikkMerge : JsonMigration(version = 21) {
    override val description = "Legger inn en sykdomstidslinje i arbeidsgiver som er sammensatt fra vedtaksperioder"

    override fun doMigration(jsonNode: ObjectNode) {
        for (arbeidsgiver in jsonNode.path("arbeidsgivere")) {
            arbeidsgiver as ObjectNode
            val originaleElementer = arbeidsgiver.path("vedtaksperioder")
                .flatMap { it.path("sykdomshistorikk") }
                .sortedBy { LocalDateTime.parse(it["tidsstempel"].asText()) }

            val elementer = (listOf(originaleElementer.first()) + originaleElementer.zipWithNext { nåværende, neste ->
                val resultat = neste.deepCopy<ObjectNode>()
                val kombinert = kombinerBeregnetTidslinje(
                    nåværende["beregnetSykdomstidslinje"],
                    neste["beregnetSykdomstidslinje"]
                )
                resultat.putArray("beregnetSykdomstidslinje")
                    .addAll(kombinert)
                resultat
            })
                .reversed()


            arbeidsgiver.withArray("sykdomshistorikk")
                .addAll(elementer)
        }

    }

    fun kombinerBeregnetTidslinje(nåværende: JsonNode, neste: JsonNode): List<JsonNode> {
        val datoerFraNeste = neste.map { it["dato"].asText() }
        return nåværende.filterNot { it["dato"].asText() in datoerFraNeste } + neste
    }
}
