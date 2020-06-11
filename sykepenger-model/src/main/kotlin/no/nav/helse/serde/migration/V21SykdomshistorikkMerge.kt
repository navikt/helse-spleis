package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDateTime

internal class V21SykdomshistorikkMerge : JsonMigration(version = 21) {
    override val description = "Legger inn en sykdomstidslinje i arbeidsgiver som er sammensatt fra vedtaksperioder"

    override fun doMigration(jsonNode: ObjectNode) {
        val elementer = jsonNode.path("arbeidsgiver")
            .flatMap { it.path("vedtaksperioder") }
            .flatMap { it.path("sykdomshistorikk") }
            .sortedBy { LocalDateTime.parse(it["tidsstempel"].asText()) }
            .groupingBy { it["id"].asText() }



    }
}
