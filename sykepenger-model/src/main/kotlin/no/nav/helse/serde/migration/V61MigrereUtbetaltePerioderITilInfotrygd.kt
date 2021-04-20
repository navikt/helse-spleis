package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory

internal class V61MigrereUtbetaltePerioderITilInfotrygd : JsonMigration(version = 61) {
    private val log = LoggerFactory.getLogger(this.javaClass.name)

    override val description: String = "Setter utbetalte forkastede perioder i TIL_INFOTRYGD til AVSLUTTET"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode
            .path("arbeidsgivere")
            .forEach { arbeidsgiver ->
                val utbetalinger = arbeidsgiver.path("utbetalinger")
                arbeidsgiver.path("forkastede")
                    .map { it.path("vedtaksperiode") }
                    .filter { it.path("tilstand").asText() == "TIL_INFOTRYGD"}
                    .forEach { forkastet ->
                        finnUtbetaling(forkastet.path("utbetalingId").asText(), utbetalinger)
                            ?.takeIf { utbetaling -> utbetaling.path("status").asText() == "UTBETALT" && utbetaling.path("type").asText() == "UTBETALING" }
                            ?.let {
                                forkastet as ObjectNode
                                forkastet.put("tilstand", "AVSLUTTET")
                                log.info("Satt forkastet vedtaksperiode ${forkastet.path("id").asText()} fra TIL_INFOTRYGD til AVSLUTTET")
                            }
                    }
            }
    }

    private fun finnUtbetaling(utbetalingId: String, utbetalinger: JsonNode) =
        utbetalinger.firstOrNull { it.path("id").asText() == utbetalingId }
}

