package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import net.logstash.logback.argument.StructuredArguments.kv
import org.slf4j.LoggerFactory

private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
internal class V324FjerneLinjerMed0KrSats : JsonMigration(324) {
    override val description = "Fjerner linjer med 0 kr i sats på feriepengeutbetalinger"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val aktørId = jsonNode.path("aktørId").asText()
        jsonNode["arbeidsgivere"].forEach { arbeidsgiver ->
            arbeidsgiver.path("feriepengeutbetalinger").forEach { feriepengeutbetaling ->
                val utbetalingId = feriepengeutbetaling.path("utbetalingId").asText()
                migrerOppdrag(aktørId, utbetalingId, feriepengeutbetaling.path("oppdrag"))
                migrerOppdrag(aktørId, utbetalingId, feriepengeutbetaling.path("personoppdrag"))
            }
        }
    }

    private fun migrerOppdrag(aktørId: String, utbetalingId: String, oppdrag: JsonNode) {
        val linje = oppdrag.path("linjer").singleOrNull() ?: return
        if (linje.path("sats").asInt() != 0) return
        (oppdrag.path("linjer") as ArrayNode).removeAll()
        sikkerlogg.info("Fjerner linje med 0 kr i sats fra oppdrag med {}", kv("utbetalingId", utbetalingId), kv("aktørId", aktørId))
    }
}
