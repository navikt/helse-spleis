package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V9FjernerGamleSykdomstidslinjer : JsonMigration(version = 9) {

    override val description = "Fjerner gamle sykdomstidslinjene"
    private val hendelsetidslinjeKey = "hendelseSykdomstidslinje"
    private val beregnetTidslinjeKey = "beregnetSykdomstidslinje"

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { periode ->
                periode.path("sykdomshistorikk").forEach { historikkElement ->
                    historikkElement as ObjectNode
                    historikkElement.remove(hendelsetidslinjeKey)
                    historikkElement.remove(beregnetTidslinjeKey)
                }
            }
        }
    }
}
