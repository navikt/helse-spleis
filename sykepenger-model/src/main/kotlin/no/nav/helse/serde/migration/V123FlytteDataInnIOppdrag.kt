package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory

internal class V123FlytteDataInnIOppdrag : JsonMigration(version = 123) {

    override val description = "Flytte avstemmingsnøkkel og overføringstidspunkt fra utbetaling til oppdrag, samt legge til oppdragstatus"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode["arbeidsgivere"].forEach { arbeidsgiver ->
            arbeidsgiver["utbetalinger"].forEach { utbetaling ->
                val arbeidsgiverOppdrag = utbetaling["arbeidsgiverOppdrag"] as ObjectNode
                utbetaling.getOrNull("overføringstidspunkt")?.also { overføringstidspunkt ->
                    arbeidsgiverOppdrag.put("overføringstidspunkt", overføringstidspunkt.asText())
                }
                utbetaling.getOrNull("avstemmingsnøkkel")?.also { avstemmingsnøkkel ->
                    arbeidsgiverOppdrag.put("avstemmingsnøkkel", avstemmingsnøkkel.asText())
                }
                val oppdragStatus = konverteringsMap[utbetaling["status"].asText()]
                if (oppdragStatus == null && arbeidsgiverOppdrag.hasNonNull("avstemmingsnøkkel")) {
                    logger.error("Mangler status for fagsystemId=${arbeidsgiverOppdrag["fagsystemId"].asText()}, utbetalingId=${utbetaling["id"].asText()}")
                } else {
                    arbeidsgiverOppdrag.put("status", oppdragStatus)
                }
            }
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger("tjenestekall")
        private fun JsonNode.getOrNull(fieldName: String) = takeIf { it.hasNonNull(fieldName) }?.get(fieldName)

        private val konverteringsMap = mapOf(
            "OVERFØRT" to "OVERFØRT",
            "UTBETALT" to "AKSEPTERT",
            "UTBETALING_FEILET" to "AVVIST",
            "ANNULLERT" to "AKSEPTERT"
        )
    }
}
