package no.nav.helse.spleis.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V42AnnulleringIUtbetaling : no.nav.helse.spleis.serde.migration.JsonMigration(version = 42) {
    override val description: String = "Benytter et flagg for å skille mellom vanlige utbetalinger og annulleringer"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: no.nav.helse.spleis.serde.migration.MeldingerSupplier) {
        jsonNode["arbeidsgivere"]
            .flatMap { it["utbetalinger"] }
            .map { it as ObjectNode }
            .forEach {
                if (it["status"].asText() == "ANNULLERT") {
                    it.put("status", "UTBETALT")
                    it.put("annullert", true)
                } else {
                    it.put("annullert", false)
                }
            }
    }
}
