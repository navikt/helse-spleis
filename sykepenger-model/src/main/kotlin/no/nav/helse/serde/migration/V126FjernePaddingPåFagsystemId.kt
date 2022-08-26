package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory

internal class V126FjernePaddingPåFagsystemId : JsonMigration(version = 126) {

    override val description = "Fjerner padding på fagsystemId"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode["arbeidsgivere"].forEach { arbeidsgiver ->
            arbeidsgiver["utbetalinger"].forEach { utbetaling ->
                fjernPadding(utbetaling.path("arbeidsgiverOppdrag"))
                fjernPadding(utbetaling.path("personOppdrag"))
            }
        }
    }

    private fun fjernPadding(oppdrag: JsonNode) {
        oppdrag as ObjectNode
        if (oppdrag["fagsystemId"].asText().endsWith("=")) {
            logger.info("Fjerner padding på fagsystemId ${oppdrag["fagsystemId"].asText()}")
            secureLog.info("Fjerner padding på fagsystemId ${oppdrag["fagsystemId"].asText()}")
            oppdrag.put("fagsystemId", oppdrag["fagsystemId"].asText().replace("=", ""))
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(V126FjernePaddingPåFagsystemId::class.java)
        private val secureLog = LoggerFactory.getLogger("tjenestekall")
    }
}
