package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V100SlettEnkeltFeriepengeutbetaling : JsonMigration(version = 100) {
    override val description: String = "Sletter en borked feriepengeutbetaling"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode["arbeidsgivere"]
            .map { it["feriepengeutbetalinger"] as ArrayNode }
            .forEach { feriepengeutbetalinger ->
                feriepengeutbetalinger.removeAll { it["utbetalingId"].asText() == "66d6a594-5b5b-41c4-be0c-1841506b594b" }
            }
    }
}
