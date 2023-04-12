package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.serde.serdeObjectMapper
import org.slf4j.LoggerFactory

internal class V235OmgjortePerioderSomRevurderinger: JsonMigration(235) {
    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
    override val description = "finner alle utbetalinger merket som revurdering som ikke skal være det"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val aktørId = jsonNode.path("aktørId").asText()
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val grupperteUtbetalinger = arbeidsgiver
                .path("utbetalinger")
                .groupBy { utbetaling -> utbetaling.path("korrelasjonsId").asText() }
                .mapValues { (_, utbetalingene) -> utbetalingene.sortedBy { utbetaling ->
                    LocalDateTime.parse(utbetaling.path("tidsstempel").asText())
                } }

            grupperteUtbetalinger
                .filterValues { utbetalingene -> utbetalingene.first().path("type").asText() == "REVURDERING" }
                .forEach { (korrelasjonsId, utbetalingene) ->
                    val first = utbetalingene.first() as ObjectNode
                    sikkerlogg.info("{} utbetaling for korrelasjonsId=$korrelasjonsId med utbetalingId=${first.path("id").asText()} er feilaktigmerket som revurdering", aktørId)
                    first.put("type", "UTBETALING")
                }
        }
    }
    private fun JsonNode.fom() = path("fom").let { LocalDate.parse(it.asText()) }
}