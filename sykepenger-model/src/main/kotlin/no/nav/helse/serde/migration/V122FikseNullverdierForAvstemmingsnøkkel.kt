package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V122FikseNullverdierForAvstemmingsnøkkel : JsonMigration(version = 122) {

    override val description = "Fikser avstemmingsnøkler hvor verdien er string null"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("utbetalinger").filter {
                it.path("avstemmingsnøkkel").isTextual && it.path("avstemmingsnøkkel").asText() == "null"
            }.forEach { utbetaling ->
                utbetaling as ObjectNode
                utbetaling.replace("avstemmingsnøkkel", utbetaling.nullNode())
            }
        }
    }
}
