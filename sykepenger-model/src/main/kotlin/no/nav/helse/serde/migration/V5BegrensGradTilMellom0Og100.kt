package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import kotlin.math.max
import kotlin.math.min

internal class V5BegrensGradTilMellom0Og100 : JsonMigration(version = 5) {
    override val description = "Skriver om sykdomsgrader utenfor 0 og 100"

    private val hendelsetidslinjeKey = "hendelseSykdomstidslinje"
    private val beregnetTidslinjeKey = "beregnetSykdomstidslinje"
    private val nyHendelsetidslinjeKey = "nyHendelseSykdomstidslinje"
    private val nyBeregnetTidslinjeKey = "nyBeregnetSykdomstidslinje"

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { periode ->
                periode.path("sykdomshistorikk").forEach { historikkElement ->
                    historikkElement[hendelsetidslinjeKey].forEach(::begrensGrad)
                    historikkElement[beregnetTidslinjeKey].forEach(::begrensGrad)
                    historikkElement[nyHendelsetidslinjeKey]["dager"].forEach(::begrensGrad)
                    historikkElement[nyBeregnetTidslinjeKey]["dager"].forEach(::begrensGrad)
                }
            }
        }
    }

    private fun begrensGrad(dag: JsonNode) {
        dag.path("grad").takeIf(JsonNode::isNumber)?.let { grad ->
            (dag as ObjectNode).put("grad", max(0.0, min(100.0, grad.asDouble())))
        }
    }
}
