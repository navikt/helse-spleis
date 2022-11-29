package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate

internal class V202NullstilleSisteArbeidsgiverdag : JsonMigration(version = 202) {
    override val description = """setter sisteArbeidsgiverdag til null dersom den er satt til LocalDate.MIN"""

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("utbetalinger").forEach { utbetaling ->
                migrerOppdrag(utbetaling.path("arbeidsgiverOppdrag"))
                migrerOppdrag(utbetaling.path("personOppdrag"))
            }
        }
    }

    private fun migrerOppdrag(oppdrag: JsonNode) {
        oppdrag as ObjectNode
        val sisteArbeidsgiverdag = oppdrag.path("sisteArbeidsgiverdag")
        if (sisteArbeidsgiverdag.isNull || sisteArbeidsgiverdag.asText() != MIN_DATO) return
        oppdrag.putNull("sisteArbeidsgiverdag")
    }

    private companion object {
        private val MIN_DATO = "${LocalDate.MIN}"
    }
}