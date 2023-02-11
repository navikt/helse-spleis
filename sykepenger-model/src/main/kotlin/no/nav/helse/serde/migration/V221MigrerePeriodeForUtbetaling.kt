package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V221MigrerePeriodeForUtbetaling: JsonMigration(221) {

    override val description = "endrer navn på opprinneligPeriodeTom til tom"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("utbetalinger").forEach { utbetaling ->
                (utbetaling as ObjectNode)
                utbetaling.put("fom", utbetaling.path("opprinneligPeriodeFom").asText())
                utbetaling.put("tom", utbetaling.path("opprinneligPeriodeTom").asText())
            }
        }
    }
}