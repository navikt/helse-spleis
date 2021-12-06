package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

internal class V129KopiereSimuleringsResultatTilOppdrag : JsonMigration(version = 129) {

    override val description =
        "Kopiere SimuleringsResultat fra vedtaksperiode over til oppdrag. Fra og med ce36471 blir SimuleringsResultat lagret begge steder"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->

            val utbetalinger = arbeidsgiver.path("utbetalinger")
                .sortedBy { LocalDateTime.parse(it["tidsstempel"].asText()) }
                .filter { it.path("arbeidsgiverOppdrag").oppdragHarUtbetalinger() || it.path("personOppdrag").oppdragHarUtbetalinger() }

            arbeidsgiver.path("vedtaksperioder")
                .filter { it.vedtaksperiodeHarSimuleringsResultat() } // Trenger ikke kopiere over fra de som ikke har noe simuleringsResultat
                .forEach { it.kopierSimuleringsResultat(utbetalinger) }
        }
    }

    private fun JsonNode.kopierSimuleringsResultat(utbetalinger: List<JsonNode>) {
        val simuleringsResultat = path("dataForSimulering")
        val utbetalingIder = path("utbetalinger").map { it.asText() }
        val sisteUtbetaling = utbetalinger.lastOrNull { it["id"].asText() in utbetalingIder }
            ?: return logger.info("Fant ingen utbetaling å migrere simuleringsResultat til for vedtaksperiode ${path("id").asText()}")
        val personOppdrag = sisteUtbetaling["personOppdrag"] as ObjectNode
        val personOppdragMottaker = personOppdrag.path("mottaker").asText()
        val arbeidsgiverOppdrag = sisteUtbetaling["arbeidsgiverOppdrag"] as ObjectNode

        if (simuleringsResultat.simuleringsResultatInneholderMottaker(personOppdragMottaker) && personOppdrag.oppdragManglerSimuleringsResultat()) {
            personOppdrag.replace("simuleringsResultat", simuleringsResultat)
            logger.info("La til simuleringsResultat for personOppdrag på utbetaling ${sisteUtbetaling["id"].asText()}")
        } else if (arbeidsgiverOppdrag.oppdragManglerSimuleringsResultat()) {
            arbeidsgiverOppdrag.replace("simuleringsResultat", simuleringsResultat)
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(V129KopiereSimuleringsResultatTilOppdrag::class.java)

        private fun JsonNode.isMissingOrNull() = isMissingNode || isNull
        private fun JsonNode.oppdragHarUtbetalinger() = path("linjer").any { "UEND" != it.path("endringskode").asText() }
        private fun JsonNode.vedtaksperiodeHarSimuleringsResultat() = path("dataForSimulering").isObject
        private fun JsonNode.oppdragManglerSimuleringsResultat() = path("simuleringsResultat").isMissingOrNull()
        private fun JsonNode.simuleringsResultatInneholderMottaker(mottaker: String) =
            path("perioder").any { periode -> periode.path("utbetalinger").any { utbetaing -> utbetaing.path("utbetalesTil").path("id").asText() == mottaker } }
    }
}
