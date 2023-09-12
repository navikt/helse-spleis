package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import net.logstash.logback.argument.StructuredArguments.kv
import org.slf4j.LoggerFactory

internal class V270ForkasteHerreløseUtbetalinger: JsonMigration(270) {
    override val description = "spisset migrering for å forkaste trøblete utbetalinger"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val utbetalingerIBruk = jsonNode.path("arbeidsgivere").flatMap { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").flatMap(::utbetalinger) + arbeidsgiver.path("forkastede").flatMap { utbetalinger(it.path("vedtaksperiode")) }
        }.toSet()
        val utbetalinger = jsonNode.path("arbeidsgivere").flatMap { arbeidsgiver ->
            arbeidsgiver.path("utbetalinger")
                .filter { it.path("type").asText() == "UTBETALING" }
                .filter { it.path("status").asText() == "GODKJENT_UTEN_UTBETALING" }
                .map { utbetaling -> utbetaling.path("id").asText() }
        }.toSet()
        val ider = utbetalinger - utbetalingerIBruk
        if (ider.isEmpty()) return
        sikkerlogg.info("V270 {} kommer frem til at ${ider.size} utbetalinger ikke pekes på av noen vedtaksperiode, og blir forkastet", kv("aktørId", jsonNode.path("aktørId").asText()))
        val trøblete = TrøbleteUtbetalinger(ider)
        trøblete.doMigration(jsonNode)
    }

    private fun utbetalinger(vedtaksperiode: JsonNode) = vedtaksperiode
        .path("utbetalinger")
        .map { it.path("utbetalingId").asText() }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
