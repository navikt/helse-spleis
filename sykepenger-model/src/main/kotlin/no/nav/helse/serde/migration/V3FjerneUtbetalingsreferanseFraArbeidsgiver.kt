package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V3FjerneUtbetalingsreferanseFraArbeidsgiver : JsonMigration(version = 3) {

    override val description = "Fjerner utbetalingsreferanse fra arbeidsgiver"

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            (arbeidsgiver as ObjectNode).remove("utbetalingsreferanse")
        }
    }
}
