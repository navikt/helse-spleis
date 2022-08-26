package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V131FjernHelgedagFraSykdomstidslinje : JsonMigration(version = 131) {
    override val description = "Fjerner SYK_HELGEDAG og ARBEIDSGIVER_HELGEDAG fra sykdomstidslinjer"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode["arbeidsgivere"].forEach { arbeidsgiver ->
            arbeidsgiver["vedtaksperioder"].forEach { vedtaksperiode ->
                migrer(vedtaksperiode["sykdomstidslinje"])
            }

            arbeidsgiver["forkastede"].forEach { forkastet ->
                migrer(forkastet["vedtaksperiode"]["sykdomstidslinje"])
            }

            arbeidsgiver["sykdomshistorikk"].forEach { innslag ->
                migrer(innslag["beregnetSykdomstidslinje"])
                migrer(innslag["hendelseSykdomstidslinje"])
            }
        }

    }

    private fun migrer(sykdomstidslinje: JsonNode) {
        sykdomstidslinje["dager"].forEach { dag ->
            dag as ObjectNode
            when (dag["type"].asText()) {
                "ARBEIDSGIVER_HELGEDAG" -> dag.put("type", "ARBEIDSGIVERDAG")
                "SYK_HELGEDAG" -> dag.put("type", "SYKEDAG")
            }
        }
    }
}
