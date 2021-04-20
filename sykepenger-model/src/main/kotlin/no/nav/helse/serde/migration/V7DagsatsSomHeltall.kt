package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import kotlin.math.roundToInt

internal class V7DagsatsSomHeltall : JsonMigration(version = 7) {
    override val description = "Runder av dagsats til nærmeste krone"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode["arbeidsgivere"].forEach { arbeidsgiver ->
            arbeidsgiver["utbetalinger"].forEach { utbetaling ->
                utbetaling.path("utbetalingstidslinje")
                    .takeIf(JsonNode::isObject)?.also {
                        migrerUtbetalingstidslinje(it)
                    }
            }
            arbeidsgiver["vedtaksperioder"].forEach { vedtaksperiode ->
                vedtaksperiode.path("utbetalingstidslinje")
                    .takeIf(JsonNode::isObject)?.also {
                        migrerUtbetalingstidslinje(it)
                    }
            }
        }
    }

    private fun migrerUtbetalingstidslinje(utbetalingstidslinje: JsonNode) {
        utbetalingstidslinje.path("dager").forEach { dag ->
            (dag as ObjectNode).put("dagsats", dag
                .path("inntekt")
                .asDouble()
                .takeUnless(Double::isNaN)
                ?.roundToInt()
                ?: 0)
        }
    }
}
