package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.person.AktivitetsloggObserver

internal class V99LeggerTilSatstypePåUtbetalingslinjene : JsonMigration(version = 99) {
    override val description: String = "Legger til nytt felt satstype i utbetalingslinjene"

    override fun doMigration(
        jsonNode: ObjectNode,
        meldingerSupplier: MeldingerSupplier,
        observer: AktivitetsloggObserver
    ) {
        jsonNode["arbeidsgivere"]
            .flatMap { it["utbetalinger"] }
            .map { it["arbeidsgiverOppdrag"] }
            .flatMap { it["linjer"] }
            .forEach { (it as ObjectNode).put("satstype", "DAG") }
    }
}
