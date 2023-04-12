package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import java.time.LocalDateTime
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import org.slf4j.LoggerFactory

internal class V236MigrereUtbetalingTilÅOverlappeMedAUU: JsonMigration(236) {
    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
    override val description = "migrerer utbetaling til å overlappe med AUU"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val grupperteUtbetalinger = arbeidsgiver
                .path("utbetalinger")
                .filterNot {
                    it.path("status").asText() == "FORKASTET"
                }
                .groupBy { utbetaling -> utbetaling.path("korrelasjonsId").asText() }
                .mapValues { (_, utbetalingene) -> utbetalingene.sortedBy { utbetaling ->
                    LocalDateTime.parse(utbetaling.path("tidsstempel").asText())
                } }

            arbeidsgiver.path("vedtaksperioder").filter {
                it.path("tilstand").asText() == "AVSLUTTET_UTEN_UTBETALING"
            }.asReversed().forEach {
                val periode = it.path("fom").dato() til it.path("tom").dato()
                val overlappendeUtbetaling = grupperteUtbetalinger.entries.firstOrNull {( _, utbetalinger) ->
                    utbetalinger.any { utbetaling ->
                        val utbetalingPeriode = utbetaling.path("fom").dato() til utbetaling.path("tom").dato()
                        val dagerMellom = periode.periodeMellom(utbetalingPeriode.start)?.count()
                        val harUkjentDagMellom = utbetaling.path("utbetalingstidslinje").path("dager").filter {
                            it.path("type").asText() == "UkjentDag"
                        }.any { dag ->
                            val ukjentPeriode = when {
                                dag.hasNonNull("dato") -> dag.path("dato").dato().somPeriode()
                                else -> dag.path("fom").dato() til dag.path("tom").dato()
                            }
                            ukjentPeriode.overlapperMed(periode.oppdaterTom(utbetalingPeriode.start))
                        }
                        !harUkjentDagMellom && ((dagerMellom != null && dagerMellom <= 15) || periode.erRettFør(utbetalingPeriode))
                    }
                }
                if (overlappendeUtbetaling != null ) {
                    val nyPeriode = periode.start til overlappendeUtbetaling.value.first().path("fom").dato()
                    if (grupperteUtbetalinger.any {(korrelasjonsId, utbetalinger) ->
                            korrelasjonsId != overlappendeUtbetaling.key && utbetalinger
                                .filterNot { it.path("status").asText() == "FORKASTET"
                            }.any { it.path("fom").dato() in nyPeriode }
                        }) {
                        sikkerlogg.info("" +
                                "{} Endrer ikke fom fra ${overlappendeUtbetaling.value.first().path("fom").dato()} " +
                                "til ${periode.start} " +
                                "for utbetalingId = ${it.path("id").asText()}", keyValue("aktørId", jsonNode.path("aktørId").asText()))
                    } else {
                        overlappendeUtbetaling.value.forEach {
                            val dato = it.path("fom").dato()
                            sikkerlogg.info("" +
                                    "{} Endrer fom fra ${dato} " +
                                    "til ${periode.start} " +
                                    "for utbetalingId = ${it.path("id").asText()}", keyValue("aktørId", jsonNode.path("aktørId").asText()))
                            (it as ObjectNode)
                            it.put("fom", minOf(periode.start, dato).toString())
                        }
                    }
                }
            }
        }
    }
    private fun JsonNode.dato() = LocalDate.parse(asText())

}