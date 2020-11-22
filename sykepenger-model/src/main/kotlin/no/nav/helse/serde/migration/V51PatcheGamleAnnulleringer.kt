package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V51PatcheGamleAnnulleringer : JsonMigration(version = 51) {
    override val description: String = "Patcher gamle annulleringer av allerede annullerte utbetalinger"

    override fun doMigration(jsonNode: ObjectNode) {
        val annullerteFagsystemIder = mutableSetOf<String>()

        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("utbetalinger").forEach { utbetaling ->
                val erAnnullering = utbetaling.path("annullert").asBoolean()
                val fagsystemId = utbetaling.path("arbeidsgiverOppdrag").path("fagsystemId").asText()
                if (erAnnullering) annullerteFagsystemIder.add(fagsystemId)
                else if (annullerteFagsystemIder.contains(fagsystemId)) (utbetaling as ObjectNode).put("annullert", true)
            }
        }
    }
}

