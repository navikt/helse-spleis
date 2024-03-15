package no.nav.helse.spleis.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V99LeggerTilSatstypePåUtbetalingslinjene : JsonMigration(version = 99) {
    override val description: String = "Legger til nytt felt satstype i utbetalingslinjene"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode["arbeidsgivere"]
            .flatMap { it["utbetalinger"] }
            .map { it["arbeidsgiverOppdrag"] }
            .flatMap { it["linjer"] }
            .forEach { (it as ObjectNode).put("satstype", "DAG") }
    }
}
