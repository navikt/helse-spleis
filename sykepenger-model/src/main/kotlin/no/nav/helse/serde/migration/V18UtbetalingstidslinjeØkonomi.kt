package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V18UtbetalingstidslinjeØkonomi : JsonMigration(version = 18) {
    override val description = "befolke økonomifelt i Utbetalingstidslinjer"

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("utbetalinger").forEach { utbetaling ->
                opprettØkonomi(utbetaling.path("utbetalingstidslinje").path("dager"))
            }
            arbeidsgiver.path("vedtaksperioder").forEach { periode ->
                opprettØkonomi(periode.path("utbetalingstidslinje").path("dager"))
            }
        }
    }

    private fun opprettØkonomi(dager: JsonNode?) {
        if (dager == null) return

    }

}
