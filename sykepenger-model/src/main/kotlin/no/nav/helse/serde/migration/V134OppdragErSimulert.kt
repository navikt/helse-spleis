package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V134OppdragErSimulert : JsonMigration(version = 134) {
    override val description = "Sette erSimulert på Oppdrag"

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
        oppdrag as ObjectNode
        val erSimulert = oppdrag.hasNonNull("simuleringsResultat")
        oppdrag.put("erSimulert", erSimulert)
    }
}
