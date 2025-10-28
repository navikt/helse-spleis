package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.io.File
import java.time.LocalDate
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.serde.migration.JsonMigration.Companion.dato
import org.slf4j.LoggerFactory

internal class V339JustereFomPåSpesifikkeUtbetalinger : JsonMigration(339) {
    override val description = "Fikser fom på spesifikke utbetalinger"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val fnr = jsonNode.path("fødselsnummer").asText()
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("utbetalinger")
                .filter {
                    it.path("id").asText() in utbetalingerSomSkalFikses
                }
                .forEach { utbetaling ->
                    val (opprinneligFom, nyOgFeilFom) = utbetalingerSomSkalFikses.getValue(utbetaling.path("id").asText())
                    if (utbetaling.path("fom").dato == nyOgFeilFom) {
                        sikkerlogg.info("Endrer tilbake fom fra=$nyOgFeilFom til fom=$opprinneligFom for utbetaling med id=${utbetaling.path("id").asText()}.",)
                            kv("fødselsnummer", fnr)
                        (utbetaling as ObjectNode).putArray("fom").apply {
                            add(opprinneligFom.year)
                            add(opprinneligFom.monthValue)
                            add(opprinneligFom.dayOfMonth)
                        }
                    }
                }
        }
    }
}

private fun String.file() =
    object {}.javaClass.getResource(this)?.readText()?.takeIf { it.isNotBlank() } ?: error("did not find resource <$this>")

private val utbetalingerSomSkalFikses = "/utbetalinger.csv".file()
    .lineSequence()
    .filter { it.isNotBlank() }
    .associate { line ->
        val parts = line.split(",")
        check(parts.size == 3) { "Kan ikke tolke linjen $parts" }
        val (utbetalingId, opprinneligFom, nyOgFeilFom) = parts
        Pair(utbetalingId, Pair(opprinneligFom.dato, nyOgFeilFom.dato))
    }
    .toMap()

private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

private val JsonNode.dato get() = when {
    isTextual -> asText().dato
    isArray -> LocalDate.of(this[0].asInt(), this[1].asInt(), this[2].asInt())
    else -> error("Ukjent datoformat: $this")
}
