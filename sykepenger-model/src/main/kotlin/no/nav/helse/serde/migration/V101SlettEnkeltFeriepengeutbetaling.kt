package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V101SlettEnkeltFeriepengeutbetaling : JsonMigration(version = 101) {
    override val description: String = "Sletter en borked feriepengeutbetaling"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode["arbeidsgivere"]
            .map { it["feriepengeutbetalinger"] }
            .filterIsInstance<ArrayNode>()
            .forEach { feriepengeutbetalinger ->
                feriepengeutbetalinger.removeAll { it["utbetalingId"].asText() == "6fbdf0a8-e32f-4fb1-b1e2-14757ed9d4c7" }
            }
    }
}
