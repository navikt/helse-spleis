package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDateTime
import org.slf4j.LoggerFactory

internal class V285LoggeRareAnnulleringer: JsonMigration(version = 285) {
    override val description = "Logger rare utbetalinger"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val aktørId = jsonNode.path("aktørId").asText()

        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val organisasjonsnummer = arbeidsgiver.path("organisasjonsnummer").asText()
            arbeidsgiver.path("utbetalinger")
                .filter { it.path("type").asText() == "ANNULLERING" }
                .filter { it.path("status").asText() != "FORKASTET" }
                .filter { it.path("overføringstidspunkt").isNull }
                .forEach { rarAnnullering ->
                    val id = rarAnnullering.path("id").asText()
                    val nåværendeStatus = rarAnnullering.path("status").asText()
                    val oppdatert = LocalDateTime.parse(rarAnnullering.path("oppdatert").asText())
                    sikkerLogg.warn("Annullering $id i Status $nåværendeStatus ser ikke ut til å være overført til Oppdrag (sist oppdatert $oppdatert). AktørId $aktørId, Organisasjonsnummer $organisasjonsnummer")
                    if (id == testkanin) {
                        sikkerLogg.info("Setter status på $id til ")
                        rarAnnullering as ObjectNode
                        rarAnnullering.put("status","FORKASTET")
                    }
                }
        }
    }

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
        private val testkanin = "ec293bc9-73ce-4c5f-b7a8-18451f5f623c"
    }
}