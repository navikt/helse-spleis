package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory

internal class V111RiktigStatusAnnullerteUtbetalinger() : JsonMigration(version = 111) {
    override val description: String = "Oppdatere status på annullerte utbetalinger"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode["arbeidsgivere"]
            .forEach { arbeidsgiver ->
                arbeidsgiver["utbetalinger"]
                    .filter { it["status"]?.asText() == "UTBETALT" && it["type"]?.asText() == "ANNULLERING" }
                    .forEach { utbetaling ->
                        utbetaling as ObjectNode
                        utbetaling.put("status", "ANNULLERT")
                    }
            }
    }
}
