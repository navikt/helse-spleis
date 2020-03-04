package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V13FjernerGradFraArbeidsgiverperiodeDag : JsonMigration(version = 13) {

    override val description = "Fjerner grad på ArbeidsgiverperiodeDager"

    private val arbeidsgiverperiodeDag = "ArbeidsgiverperiodeDag"

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("utbetalingstidslinjer").forEach { utbetalingstidslinje ->
                migrerUtbetalingstidslinje(utbetalingstidslinje.path("dager"))
            }
        }
    }

    private fun migrerUtbetalingstidslinje(tidslinje: JsonNode) {
        tidslinje.forEach { dag ->
            if (dag["type"].textValue() == arbeidsgiverperiodeDag) {
                (dag as ObjectNode).remove("grad")
            }
        }
    }
}
