package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory

/**
 * Retter opp en mangel ved V55.
 */
internal class V60FiksStatusPåUtbetalinger : JsonMigration(version = 60) {
    private val log = LoggerFactory.getLogger(this.javaClass.name)

    override val description: String = "Setter riktig status på utbetalinger"

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode
            .path("arbeidsgivere")
            .forEach { arbeidsgiver ->
                arbeidsgiver
                    .path("utbetalinger")
                    .filter { it.path("status").asText() == "IKKE_GODKJENT" }
                    .onEach { utbetaling ->
                        val tilhørendeVedtaksperiode = arbeidsgiver.path("forkastede")
                            .map { it.path("vedtaksperiode") }
                            .find { it.path("utbetalingId").asText() == utbetaling.path("id").asText() }
                            ?: return@onEach
                        val aktivitetslogg = jsonNode.path("aktivitetslogg")
                        val kontekstinnslagIndex = aktivitetslogg.path("kontekster").indexOfFirst { kontekst ->
                            kontekst.path("kontekstType").asText() == "Vedtaksperiode" &&
                                kontekst.path("kontekstMap").path("vedtaksperiodeId") == tilhørendeVedtaksperiode.path("id")
                        }
                        aktivitetslogg.path("aktiviteter").find { logginnslag ->
                            logginnslag.path("melding").asText().contains("Utbetaling markert som godkjent") &&
                                logginnslag.path("kontekster").map(JsonNode::asInt).contains(kontekstinnslagIndex)
                        }?.let { logginnslag ->
                            log.info("Markerer utbetaling ${utbetaling["id"].asText()} som UTBETALT")
                            utbetaling as ObjectNode
                            utbetaling.put("status", "UTBETALT")
                            if (logginnslag.path("melding").asText().contains("Utbetaling markert som godkjent automatisk")) {
                                log.info("Markerer utbetaling ${utbetaling["id"].asText()} som automatisk godkjent")
                                (utbetaling.path("vurdering") as ObjectNode).put("automatiskBehandling", true)
                            }
                        }

                    }
            }
    }
}

