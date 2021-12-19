package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V135UtbetalingslinjeGrad : JsonMigration(version = 135) {
    override val description = "Migrerer grad på utbetalingslinjer"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("feriepengeutbetalinger").forEach { utbetaling ->
                migrer(utbetaling.path("oppdrag"))
            }
            arbeidsgiver.path("utbetalinger").forEach { utbetaling ->
                migrer(utbetaling.path("arbeidsgiverOppdrag"))
                migrer(utbetaling.path("personOppdrag"))
            }
        }
    }

    private fun migrer(oppdrag: JsonNode) {
        oppdrag.path("linjer")
            .filter { it.hasNonNull("grad") }
            .forEach { linje ->
                linje as ObjectNode
                linje.put("grad", linje.path("grad").asInt())
            }
    }
}
