package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V67FeilStatusOgTypePåAnnulleringer : JsonMigration(version = 67) {
    override val description: String = "Endrer status og type på automatisk genererte utbetalte annulleringer"

    override fun doMigration(jsonNode: ObjectNode) {
        // Denne var for inngripende. Vi må komme igjen sterkere om det enda er et problem
    }
}
