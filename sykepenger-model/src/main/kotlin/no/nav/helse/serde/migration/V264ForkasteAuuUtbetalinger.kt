package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.kv
import org.slf4j.LoggerFactory

internal class V264ForkasteAuuUtbetalinger : JsonMigration(version = 264) {
    override val description = "forkaster utbetalinger som skulle vært forkastet"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val aktørId = jsonNode.path("aktørId").asText()
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val utbetalinger = arbeidsgiver.path("utbetalinger")
                .groupBy { UUID.fromString(it.path("korrelasjonsId").asText()) }

            arbeidsgiver.path("vedtaksperioder")
                .filter { it.path("tilstand").asText() == "AVSLUTTET_UTEN_UTBETALING" }
                .filter { !it.path("utbetalinger").isEmpty }
                .forEach { periode ->
                    periode.path("utbetalinger").forEach { generasjon ->
                        val utbetalingId = UUID.fromString(generasjon.path("utbetalingId").asText())

                        val ufiltrertGruppe = utbetalinger.entries.first { (_, utbetalingerMedSammeKorrelasjonsId) ->
                            utbetalingerMedSammeKorrelasjonsId.any { it.path("id").asText().uuid == utbetalingId }
                        }.value
                        val utbetalingen = ufiltrertGruppe.first { it.path("id").asText().uuid == utbetalingId } as ObjectNode
                        val status = utbetalingen.path("status").asText()
                        if (status != "FORKASTET") {
                            utbetalingen.put("status", "FORKASTET")
                            sikkerlogg.info("V263 {} {} i AUU har utbetaling med status=$status som kan forkastes", kv("aktørId", aktørId), kv("vedtaksperiodeId", periode.path("id").asText()))
                        }
                    }
                }
        }
    }


    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}