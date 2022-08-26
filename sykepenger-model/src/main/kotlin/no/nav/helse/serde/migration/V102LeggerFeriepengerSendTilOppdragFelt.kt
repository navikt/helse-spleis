package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V102LeggerFeriepengerSendTilOppdragFelt : JsonMigration(version = 102) {
    override val description: String = "Legger til felt på feriepengerutbetalinger som er sendt til oppdrag"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode["arbeidsgivere"]
            .map { it["feriepengeutbetalinger"] }
            .filterIsInstance<ArrayNode>()
            .forEach { feriepengeutbetalinger ->
                feriepengeutbetalinger.forEach { feriepengeutbetaling ->
                    val sendtTilOppdrag = (feriepengeutbetaling as ObjectNode)["oppdrag"]["nettoBeløp"].asInt() != 0
                    feriepengeutbetaling.put("sendTilOppdrag", sendtTilOppdrag)
                }
            }
    }
}
