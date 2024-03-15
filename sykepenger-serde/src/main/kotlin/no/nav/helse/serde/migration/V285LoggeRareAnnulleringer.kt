package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDateTime
import org.slf4j.LoggerFactory

internal class V285LoggeRareAnnulleringer(
    private val forkast: Set<String> = setOf("ec293bc9-73ce-4c5f-b7a8-18451f5f623c"),
    val forkastetTidspunkt: () -> LocalDateTime = { LocalDateTime.now() }
): JsonMigration(version = 285) {
    override val description = "Logger rare utbetalinger"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val aktørId = jsonNode.path("aktørId").asText()

        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val organisasjonsnummer = arbeidsgiver.path("organisasjonsnummer").asText()
            val utbetalteKorrelasjonsIder = arbeidsgiver.path("utbetalinger")
                .groupBy { it.path("korrelasjonsId").asText() }
                .mapValues { (_, utbetalinger) ->
                    utbetalinger.filter { it.path("type").asText() == "UTBETALING" && it.path("status").asText() == "UTBETALT" }
                }.filterValues { it.isNotEmpty() }
                .keys

            val annullerteKorrelasjonsIder = arbeidsgiver.path("utbetalinger")
                .groupBy { it.path("korrelasjonsId").asText() }
                .mapValues { (_, utbetalinger) ->
                    utbetalinger.filter { it.akseptertAnnullering }
                }.filterValues { it.isNotEmpty() }
                .keys

            arbeidsgiver.path("utbetalinger")
                .asSequence()
                .filter { it.path("type").asText() == "ANNULLERING" }
                .filterNot { it.path("status").asText() == "FORKASTET" }
                .filter { LocalDateTime.parse(it.path("oppdatert").asText()).year < 2024 }
                .filter { it.path("arbeidsgiverOppdrag").oppdragManglerOverføring || it.path("personOppdrag").oppdragManglerOverføring }
                .filter { it.path("korrelasjonsId").asText() in utbetalteKorrelasjonsIder }
                .filterNot { it.path("korrelasjonsId").asText() in annullerteKorrelasjonsIder }
                .forEach { manglerOverføring ->
                    val id = manglerOverføring.path("id").asText()
                    val nåværendeStatus = manglerOverføring.path("status").asText()
                    val oppdatert = LocalDateTime.parse(manglerOverføring.path("oppdatert").asText())
                    sikkerLogg.warn("Annullering $id i Status $nåværendeStatus ser ikke ut til å være overført til Oppdrag (sist oppdatert $oppdatert). AktørId $aktørId, Organisasjonsnummer $organisasjonsnummer (Versjon4)")
                    if (id in forkast) {
                        sikkerLogg.info("Setter status på Utbetaling $id til FORKASTET og setter nytt oppdatert-tidspunkt")
                        manglerOverføring as ObjectNode
                        manglerOverføring.put("status","FORKASTET")
                        manglerOverføring.put("oppdatert", "${forkastetTidspunkt()}")
                    }
                }
        }
    }

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
        private val JsonNode.oppdragManglerOverføring get(): Boolean {
            if (path("linjer").isEmpty) return false
            if (path("endringskode").asText() == "UEND") return false
            return path("status").isNull
        }

        private val JsonNode.akseptertAnnullering get(): Boolean {
            if (path("type").asText() != "ANNULLERING") return false
            if (path("status").asText() != "ANNULLERT") return false
            val arbeidsgiverOppdrag = path("arbeidsgiverOppdrag")
            val personOppdrag = path("personOppdrag")
            return arbeidsgiverOppdrag.akseptertOppdrag && personOppdrag.akseptertOppdrag
        }

        private val JsonNode.akseptertOppdrag get(): Boolean {
            if (path("linjer").isEmpty) return true
            if (path("endringskode").asText() == "UEND") return true
            return path("status").asText() == "AKSEPTERT"
        }
    }
}