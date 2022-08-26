package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory

internal class V128FjerneUgyldigSimuleringsResultat : JsonMigration(version = 128) {
    override val description = "Fjerne ugyldige simuleringsResultat som ble lagret på oppdragene ifbm. 846541b"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode["arbeidsgivere"].forEach { arbeidsgiver ->
            arbeidsgiver["utbetalinger"].forEach { utbetaling ->
                val arbeidsgiverOppdrag = utbetaling["arbeidsgiverOppdrag"] as ObjectNode
                val personOppdrag = utbetaling["personOppdrag"] as ObjectNode
                fjernSimuleringsResultat(arbeidsgiverOppdrag)
                fjernSimuleringsResultat(personOppdrag)
            }
        }
    }

    private fun fjernSimuleringsResultat(oppdrag: ObjectNode) {
        if (oppdrag.remove("simuleringsResultat") != null) {
            logger.info("Fjernet simuleringsResultat på oppdrag med fagsystemId=${oppdrag["fagsystemId"].asText()}, overføringstidspunkt=${oppdrag.getOrNull("overføringstidspunkt")?.asText()}")
        }
    }

    private companion object {
        private fun JsonNode.getOrNull(fieldName: String) = takeIf { it.hasNonNull(fieldName) }?.get(fieldName)
        private val logger = LoggerFactory.getLogger("tjenestekall")
    }
}
