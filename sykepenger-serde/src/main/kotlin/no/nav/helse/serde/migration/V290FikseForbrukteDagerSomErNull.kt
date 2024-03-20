package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import net.logstash.logback.argument.StructuredArguments.kv
import org.slf4j.LoggerFactory
import org.slf4j.MDC

internal class V290FikseForbrukteDagerSomErNull: JsonMigration(version = 290) {
    override val description = "fikser historiske utbetalinger som har forbrukteSykedager og gjenståendeSykedager satt til NULL"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val aktørId = jsonNode.path("aktørId").asText()
        val fnr = jsonNode.path("fødselsnummer").asText()
        MDC.putCloseable("aktørId", aktørId).use {
            jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
                val orgnr = arbeidsgiver.path("organisasjonsnummer").asText()
                MDC.putCloseable("orgnr", orgnr).use {
                    arbeidsgiver.path("utbetalinger")
                        .filter { utbetaling -> utbetaling.path("type").asText() == "UTBETALING" }
                        .filter { utbetaling -> !utbetaling.hasNonNull("forbrukteSykedager") || !utbetaling.hasNonNull("gjenståendeSykedager") }
                        .forEach { utbetaling ->
                            utbetaling as ObjectNode
                            sikkerLogg.info("migrerer forbrukteSykedager/gjenståendeSykedager for ${utbetaling.path("id").asText()}")
                            if (!utbetaling.hasNonNull("forbrukeSykedager"))
                                utbetaling.put("forbrukteSykedager", 0)
                            if (!utbetaling.hasNonNull("gjenståendeSykedager"))
                                utbetaling.put("gjenståendeSykedager", 0)
                        }
                }
            }
        }
    }

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    }
}