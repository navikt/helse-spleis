package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory

internal class V67FeilStatusOgTypePåAnnulleringer : JsonMigration(version = 67) {
    override val description: String = "Endrer status og type på automatisk genererte utbetalte annulleringer"
    private val log = LoggerFactory.getLogger(this.javaClass.name)

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode
            .path("arbeidsgivere")
            .forEach { arbeidsgiver ->
                arbeidsgiver
                    .path("utbetalinger")
                    .filter { utbetaling -> utbetaling.hasNonNull("arbeidsgiverOppdrag") }
                    .filter { utbetaling -> !utbetaling["arbeidsgiverOppdrag"]["linjer"].isEmpty }
                    .filter { utbetaling -> utbetaling["arbeidsgiverOppdrag"]["linjer"][0]["statuskode"].textValue() == "OPPH" }
                    .filter { utbetaling -> utbetaling["status"].textValue() == "UTBETALT" && utbetaling["type"].textValue() == "UTBETALING" }
                    .forEach { utbetaling ->
                        utbetaling as ObjectNode
                        utbetaling.put("status", "FORKASTET")
                        utbetaling.put("type", "ANNULLERING")
                        log.info(
                            "Endret utbetaling: ${utbetaling["id"].textValue()} med fagsystemId: ${utbetaling["arbeidsgiverOppdrag"]["fagsystemId"].textValue()} " +
                                "fra UTBETALT:UTBETALING til FORKASTET:ANNULLERING"
                        )
                    }
            }
    }
}
