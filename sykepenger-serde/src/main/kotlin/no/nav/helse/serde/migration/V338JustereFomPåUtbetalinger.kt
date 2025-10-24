package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.serde.migration.JsonMigration.Companion.dato
import org.slf4j.LoggerFactory

internal class V338JustereFomPåUtbetalinger : JsonMigration(338) {
    override val description = "Lar ikke AUU-perioder overlappe med utbetalinger"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val fnr = jsonNode.path("fødselsnummer").asText()
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val aktivePerioder = arbeidsgiver.path("vedtaksperioder")
            val forkastetPerioder = arbeidsgiver.path("forkastede").map { forkastet -> forkastet.path("vedtaksperiode") }

            val vedtaksperioder = (aktivePerioder + forkastetPerioder)
                .filter { vedtaksperiode ->
                    vedtaksperiode.path("tilstand").asText() != "AVSLUTTET_UTEN_UTBETALING"
                }
                .map { vedtaksperiode ->
                    val sisteEndring = vedtaksperiode.path("behandlinger").last().path("endringer").last()
                    val fom = sisteEndring.path("fom").dato
                    val tom = sisteEndring.path("tom").dato
                    Pair(fom, tom)
                }

            arbeidsgiver.path("utbetalinger")
                .filter { it.path("status").asText() in setOf("OVERFØRT", "UTBETALT", "GODKJENT_UTEN_UTBETALING", "ANNULLERT") }
                .groupBy { it.path("korrelasjonsId").asText() }
                .forEach { (_, utbetalinger) ->
                    val siste = utbetalinger.last()

                    val utbetalingFom = siste.path("fom").dato
                    val utbetalingTom = siste.path("tom").dato
                    val utbetalingperiode = Pair(utbetalingFom, utbetalingTom)

                    vedtaksperioder
                        .filter { vedtaksperiodeSomIkkeErAuu ->
                            utbetalingperiode.overlapperMed(vedtaksperiodeSomIkkeErAuu)
                        }
                        .takeIf { it.isNotEmpty() }
                        ?.let { overlappendePerioder ->
                            val minsteFom = overlappendePerioder.minOf { it.first }

                            if (utbetalingFom != minsteFom && minsteFom <= utbetalingTom) {
                                sikkerlogg.info("Endrer fom fra=$utbetalingFom til fom=$minsteFom for utbetaling med id=${siste.path("id").asText()} fordi den overlappet med AUU-perioder.",
                                    kv("fødselsnummer", fnr))
                                (siste as ObjectNode).putArray("fom").apply {
                                    add(minsteFom.year)
                                    add(minsteFom.monthValue)
                                    add(minsteFom.dayOfMonth)
                                }
                            }
                        }
                }
        }
    }
}

private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

private fun Pair<LocalDate, LocalDate>.overlapperMed(other: Pair<LocalDate, LocalDate>): Boolean {
    val start = maxOf(this.first, other.first)
    val slutt = minOf(this.second, other.second)
    return start <= slutt
}

private val JsonNode.dato get() = when {
    isTextual -> asText().dato
    isArray -> LocalDate.of(this[0].asInt(), this[1].asInt(), this[2].asInt())
    else -> error("Ukjent datoformat: $this")
}
