package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDateTime
import net.logstash.logback.argument.StructuredArguments.keyValue

internal class V208GjenoppliveAUU: GjenopplivingAvTidligereForkastet(version = 208) {
    override fun finnPerioder(jsonNode: ObjectNode): Set<String> {
        val vedtaksperioder = mutableSetOf<String>()

        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val utbetalinger = arbeidsgiver.path("utbetalinger")
                .groupBy { utbetaling -> utbetaling.path("korrelasjonsId").asText() }
                .mapValues { (_, utbetalinger) -> utbetalinger.sortedBy { LocalDateTime.parse(it.path("tidsstempel").asText()) } }

            arbeidsgiver.path("forkastede").forEach { forkastetPeriode ->
                val vedtaksperiode = forkastetPeriode.path("vedtaksperiode")
                val tilstand = vedtaksperiode.path("tilstand").asText()

                // potensiell kandidat for gjenoppliving
                if (tilstand == "AVSLUTTET_UTEN_UTBETALING") {
                    val utbetalingId = vedtaksperiode.path("utbetalinger").lastOrNull()?.path("utbetalingId")?.asText()
                    val utbetaling = utbetalingId?.let { id ->
                        utbetalinger.firstNotNullOfOrNull { (_, utbetalinger) ->
                            utbetalinger.lastOrNull()?.takeIf {
                                utbetalinger.any { utbetaling ->
                                    utbetaling.path("id").asText() == id
                                }
                            }
                        }
                    }
                    if (utbetalingId == null || utbetaling == null || utbetaling.path("status").asText() != "ANNULLERT") {
                        vedtaksperioder.add(vedtaksperiode.path("id").asText())
                    } else {
                        val aktørId = jsonNode.path("aktørId").asText()
                        sikkerLogg.info("{} gjenoppliver ikke {} fordi den har anullert utbetaling",
                            keyValue("aktørId", aktørId), vedtaksperiode.path("id").asText())
                    }
                }
            }
        }
        return vedtaksperioder.toSet()
    }
}