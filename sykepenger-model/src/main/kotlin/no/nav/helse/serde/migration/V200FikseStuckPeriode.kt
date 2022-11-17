package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V200FikseStuckPeriode : JsonMigration(version = 200) {
    override val description = """rydder opp i en stuck periode"""

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val arbeidsgiver = jsonNode.path("arbeidsgivere").firstOrNull { it.path("id").asText() == targetArbeidsgiverId } ?: return
        val utbetaling = arbeidsgiver.path("utbetalinger").firstOrNull { it.path("id").asText() == targetUtbetalingId } ?: return
        (utbetaling as ObjectNode).put("status", "FORKASTET")
    }

    private companion object {
        private const val targetArbeidsgiverId = "e17c76d2-08e0-4f4d-b145-2125d9e61266"
        private const val targetUtbetalingId = "12fcfb7a-7d91-4901-ad4b-b28e8211d6cb"
    }
}