package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal class V54UtvideUtbetaling : JsonMigration(version = 54) {
    override val description: String = "Utvider Utbetaling med maksdato, forbrukte og gjenstående sykedager"

    private val log = LoggerFactory.getLogger("tjenestekall")

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val aktive = arbeidsgiver.path("vedtaksperioder").toList()
            val forkastede = arbeidsgiver.path("forkastede").map { it.path("vedtaksperiode") }
            val detaljer = (aktive + forkastede)
                .filter { it.hasNonNull("utbetalingId") }
                .map { periode ->
                    periode.path("utbetalingId").asText() to Triple(
                        first = periode.path("maksdato").asText(),
                        second = periode.path("forbrukteSykedager").asInt(),
                        third = periode.path("gjenståendeSykedager").asInt()
                    )
                }.toMap()

            arbeidsgiver
                .path("utbetalinger")
                .forEach { (it as ObjectNode).put("maksdato", "${LocalDate.MAX}") }

            arbeidsgiver
                .path("utbetalinger")
                .filter { detaljer.containsKey(it.path("id").asText()) }
                .map { it as ObjectNode }
                .forEach {
                    val (maksdato, forbrukteSykedager, gjenståendeSykedager) = detaljer.getValue(it.path("id").asText())
                    it.put("maksdato", maksdato)
                    it.put("forbrukteSykedager", forbrukteSykedager)
                    it.put("gjenståendeSykedager", gjenståendeSykedager)
                }

            arbeidsgiver
                .path("utbetalinger")
                .filterNot { detaljer.containsKey(it.path("id").asText()) }
                .filterNot { it.path("annullert").asBoolean() }
                .onEach {
                    log.info("Fant ikke detaljer for utbetaling=${it.path("id").asText()}")
                }
        }
    }
}

