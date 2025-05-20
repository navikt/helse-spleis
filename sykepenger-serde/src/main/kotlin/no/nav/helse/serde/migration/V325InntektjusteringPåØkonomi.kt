package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V325InntektjusteringPåØkonomi : JsonMigration(325) {
    override val description = "setter beregningsgrunnlag lik aktuellDagsinntekt på alle økonomiobjekter"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("utbetalinger")
                .onEach { utbetaling -> migrerUtbetaling(utbetaling) }
            arbeidsgiver.path("vedtaksperioder").forEach { migrerVedtaksperiode(it) }
            arbeidsgiver.path("forkastede").forEach { migrerVedtaksperiode(it.path("vedtaksperiode")) }
        }
    }

    private fun migrerVedtaksperiode(vedtaksperiode: JsonNode) {
        vedtaksperiode.path("behandlinger").forEach { behandling ->
            behandling.path("endringer").forEach { endring ->
                migrerUtbetalingstidslinje(endring.path("utbetalingstidslinje"))
            }
        }
    }

    private fun migrerUtbetaling(utbetaling: JsonNode) {
        migrerUtbetalingstidslinje(utbetaling.path("utbetalingstidslinje"))
    }

    private fun migrerUtbetalingstidslinje(utbetalingstidslinje: JsonNode) {
        utbetalingstidslinje.path("dager").forEach { dag ->
            dag as ObjectNode
            dag.put("inntektjustering", 0.0)
        }
    }
}
