package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

internal class V81ForkastetUtbetalinger : JsonMigration(version = 81) {
    private companion object {
        const val IkkeUtbetalt = "IKKE_UTBETALT"
        const val Forkastet = "FORKASTET"
        val cutoffDate: LocalDateTime = LocalDate.of(2020, 12, 31).atTime(LocalTime.MIDNIGHT)
    }
    override val description: String = "Migrerer gamle IKKE_UTBETALT til FORKASTET."

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("utbetalinger").forEach { element ->
                migrateUtbetaling(element as ObjectNode)
            }
        }
    }

    private fun migrateUtbetaling(element: ObjectNode) {
        if (element.path("status").asText() != IkkeUtbetalt) return
        val tidsstempel = LocalDateTime.parse(element.path("tidsstempel").asText())
        if (tidsstempel > cutoffDate) return
        element.put("status", Forkastet)
    }
}

