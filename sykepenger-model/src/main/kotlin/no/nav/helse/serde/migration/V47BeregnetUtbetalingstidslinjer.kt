package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V47BeregnetUtbetalingstidslinjer : JsonMigration(version = 47) {
    override val description: String = "Lager lister over beregnede utbetalingstidslinjer"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val linjer = (arbeidsgiver as ObjectNode).putArray("beregnetUtbetalingstidslinjer")

            arbeidsgiver.path("utbetalinger").forEach { utbetaling ->
                linjer.addObject()
                    .put("organisasjonsnummer", arbeidsgiver.path("organisasjonsnummer").asText())
                    .put("tidsstempel", utbetaling.path("tidsstempel").asText())
                    .set<ObjectNode>("utbetalingstidslinje", (utbetaling.path("utbetalingstidslinje") as ObjectNode))
            }
        }
    }
}

