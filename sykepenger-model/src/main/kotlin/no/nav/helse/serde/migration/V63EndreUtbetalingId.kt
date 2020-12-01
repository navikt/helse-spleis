package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V63EndreUtbetalingId : JsonMigration(version = 63) {
    override val description: String = "Endrer ID på en utbetaling som overskrev en annen grunnet race condition"

    private val existingId = "3264ed40-ca5b-4af0-8591-5ee74df8df89"
    private val newId = "bd61804d-2e98-427e-bc78-f494fd9747d6"

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode
            .path("arbeidsgivere")
            .forEach { arbeidsgiver ->
                arbeidsgiver
                    .path("utbetalinger")
                    .firstOrNull { it.path("id").asText() == existingId }
                    ?.let { utbetaling ->
                        (utbetaling as ObjectNode).also {
                            it.put("id", newId)
                            it.put("status", "SENDT")
                        }
                    }
            }
    }
}

