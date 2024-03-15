package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V283BeregningsgrunnlagPåØkonomi: JsonMigration(283) {
    override val description = "setter beregningsgrunnlag lik aktuellDagsinntekt på alle økonomiobjekter"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        migrer(jsonNode, jsonNode.path("aktørId").asText())
    }

    private fun migrer(jsonNode: ObjectNode, aktørId: String) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("utbetalinger")
                .onEach { utbetaling -> migrerUtbetaling(aktørId, utbetaling) }
        }
    }
    private fun migrerUtbetaling(aktørId: String, utbetaling: JsonNode) {
        utbetaling.path("utbetalingstidslinje").path("dager").forEach { dag ->
            dag as ObjectNode
            dag.put("beregningsgrunnlag", dag.path("aktuellDagsinntekt").asDouble())
        }
    }
}
