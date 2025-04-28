package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.kv
import org.slf4j.LoggerFactory

private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
internal class V323FikseFomPåUtbetalinger : JsonMigration(323) {
    override val description = "Fikser fom på utbetalinger"
    private val skadeomfangstidsrom = LocalDateTime.of(2025, 4, 24, 15, 13, 0) .. LocalDateTime.of(2025, 4, 28, 21, 20, 0)

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode["arbeidsgivere"].forEach { arbeidsgiver ->
            val fomForUtbetalinger = arbeidsgiver.path("utbetalinger")
                .map {
                    val arbeidsgiverfom = it.path("arbeidsgiverOppdrag").path("linjer").map { linje -> linje.path("fom").asText().dato }
                    val personfom = it.path("personOppdrag").path("linjer").map { linje -> linje.path("fom").asText().dato }
                    val tidligsteOppdragslinjefom = (arbeidsgiverfom + personfom).minOrNull()
                    ForenkletUtbetaling(
                        korrelasjonsId = it.path("korrelasjonsId").asText().uuid,
                        tidsstempel = LocalDateTime.parse(it.path("tidsstempel").asText()),
                        fom = it.path("fom").asText().dato,
                        tidligsteOppdragslinjefom = tidligsteOppdragslinjefom,
                        underliggende = it
                    )
                }
                .groupBy { it.korrelasjonsId }
                .mapValues { (_, utbetalinger) ->
                    utbetalinger.sortedBy { it.tidsstempel }
                }

            fomForUtbetalinger
                .forEach { (_, utbetalinger) ->
                    utbetalinger
                        .forEachIndexed { index, utbetaling ->
                            if (index > 0 && utbetaling.tidsstempel in skadeomfangstidsrom) {
                                val fomForutUtbetalingen = utbetalinger[index - 1]

                                if (utbetaling.fom != fomForutUtbetalingen.fom) {
                                    // fom har endret seg ... spørsmålet er: var det riktig?
                                    if (utbetaling.tidligsteOppdragslinjefom == fomForutUtbetalingen.tidligsteOppdragslinjefom) {
                                        // fom har nok endret seg til feil ja
                                        sikkerlogg.info("fikser fom for utbetaling ${utbetaling.underliggende.path("id").asText()} fra ${utbetaling.fom} til ${fomForutUtbetalingen.fom} for {}", kv("aktørId", jsonNode.path("aktørId").asText()))
                                        (utbetaling.underliggende as ObjectNode).put("fom", fomForutUtbetalingen.fom.toString())
                                    }
                                }
                            }
                        }
                }
        }
    }

    private data class ForenkletUtbetaling(
        val korrelasjonsId: UUID,
        val tidsstempel: LocalDateTime,
        val fom: LocalDate,
        val tidligsteOppdragslinjefom: LocalDate?,
        val underliggende: JsonNode
    )
}
