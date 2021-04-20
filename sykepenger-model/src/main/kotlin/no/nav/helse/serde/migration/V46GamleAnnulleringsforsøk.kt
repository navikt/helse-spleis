package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory

internal class V46GamleAnnulleringsforsøk : JsonMigration(version = 46) {
    override val description: String = "Patcher annullering som sitter fast fra gammel annulleringsløype"
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    /*
     * Disse utbetalingene sjekkes og annulleres utenfor spleis og vi anter herfra at de er ferdig annullert
     */

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("utbetalinger")
                .filter { it["arbeidsgiverOppdrag"]["linjer"].size() == 1 }
                .filter { it["arbeidsgiverOppdrag"]["linjer"].first().path("statuskode").asText(null) == "OPPH" }
                .filter { !(it.takeIf { it.hasNonNull("annullert") }?.booleanValue() ?: false) }
                .forEach {
                    (it as ObjectNode).put("annullert", true)
                    sikkerLogg.info("Patchet gammel annullering på ${jsonNode["fødselsnummer"].textValue()}, utbetalingsref: ${it["arbeidsgiverOppdrag"]["fagsystemId"].textValue()}")
                }
        }
    }
}

