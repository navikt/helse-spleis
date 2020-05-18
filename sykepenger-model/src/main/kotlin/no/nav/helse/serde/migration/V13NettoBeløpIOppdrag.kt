package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V13NettoBeløpIOppdrag : JsonMigration(version = 13) {

    override val description = "Legger til netto beløp i oppdrag"

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode["arbeidsgivere"].forEach { arbeidsgiver ->
            arbeidsgiver["utbetalinger"].forEach { utbetaling ->
                arrayOf(utbetaling["arbeidsgiverOppdrag"], utbetaling["personOppdrag"]).forEach { oppdrag ->
                    oppdrag as ObjectNode
                    oppdrag.put("nettoBeløp", 0)
                }
            }
        }
    }
}
