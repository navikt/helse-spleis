package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

internal class V50KnytteVedtaksperiodeTilUtbetaling : JsonMigration(version = 50) {
    private companion object {
        private val tidsstempelformat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    }

    override val description: String = "Knytter en vedtaksperiode til en utbetaling"

    private val vedtaksperiodeKontekst = "Vedtaksperiode"
    private val vedtaksperiodeId = "vedtaksperiodeId"
    private val tekster = listOf("Utbetalingslinjer bygget vellykket", "Ingen utbetalingslinjer bygget")

    private val maxDiff = TimeUnit.SECONDS.toMillis(1)
    private val minDiff = -maxDiff

    private val log = LoggerFactory.getLogger("tjenestekall")

    override fun doMigration(jsonNode: ObjectNode) {
        val konteksttyper = konteksttyper(jsonNode)
        val kontekstdetaljer = kontekstdetaljer(jsonNode)

        val tidsstempelTilVedtaksperiodeId = utbetalingmeldinger(jsonNode)
            .map { aktivitet ->
                val vedtaksperiodeId = finnVedtaksperiodeId(konteksttyper, kontekstdetaljer, aktivitet)
                LocalDateTime.parse(aktivitet.path("tidsstempel").asText(), tidsstempelformat) to vedtaksperiodeId
            }
            .sortedBy { it.first }

        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("utbetalinger").forEach { utbetaling ->
                val utbetalingId = utbetaling.path("id").asText()
                val utbetalingTidsstempel = LocalDateTime.parse(utbetaling.path("tidsstempel").asText())
                val utbetalingStatus = utbetaling.path("status").asText()
                val erAnnullering = utbetaling.path("annullert").asBoolean()
                tidsstempelTilVedtaksperiodeId.lastOrNull { (aktivitetTidsstempel, _) ->
                    val diff = ChronoUnit.MILLIS.between(aktivitetTidsstempel, utbetalingTidsstempel)
                    diff in minDiff..maxDiff
                }?.second?.let { vedtaksperiodeId ->
                    log.info("Koblet vedtaksperiodeId=$vedtaksperiodeId til utbetaling=$utbetalingId (status=$utbetalingStatus erAnnullering=$erAnnullering opprettet=$utbetalingTidsstempel)")
                    lagreUtbetalingIdPåVedtaksperiode(arbeidsgiver, vedtaksperiodeId, utbetalingId)
                } ?: log.info("Kunne ikke koble en vedtaksperiode til utbetaling=$utbetalingId opprettet=$utbetalingTidsstempel status=$utbetalingStatus erAnnullering=$erAnnullering")
            }
        }
    }

    private fun lagreUtbetalingIdPåVedtaksperiode(arbeidsgiver: JsonNode, vedtaksperiodeId: String, utbetalingId: String) {
        finnVedtaksperiode(arbeidsgiver.path("vedtaksperioder"), vedtaksperiodeId, utbetalingId)
        finnVedtaksperiode(arbeidsgiver.path("forkastede"), vedtaksperiodeId, utbetalingId)
    }

    private fun finnVedtaksperiode(perioder: JsonNode, vedtaksperiodeId: String, utbetalingId: String) {
        perioder
            .firstOrNull { it.path("id").asText() == vedtaksperiodeId }
            ?.lagreUtbetalingId(utbetalingId)
    }

    private fun JsonNode.lagreUtbetalingId(utbetalingId: String) {
        (this as ObjectNode).put("utbetalingId", utbetalingId)
    }

    private fun utbetalingmeldinger(jsonNode: ObjectNode): List<JsonNode> {
        return jsonNode
            .path("aktivitetslogg")
            .path("aktiviteter")
            .filter { aktivitet -> aktivitet.path("melding").asText() in tekster }
    }

    private fun kontekstdetaljer(jsonNode: ObjectNode): List<Map<String, String>> {
        return jsonNode
            .path("aktivitetslogg")
            .path("kontekster")
            .map { kontekst ->
                kontekst.path("kontekstMap")
                    .fields()
                    .asSequence()
                    .map { (key, value) -> key to value.asText() }
                    .toMap()
            }
    }

    private fun konteksttyper(jsonNode: ObjectNode): List<String> {
        return jsonNode
            .path("aktivitetslogg")
            .path("kontekster")
            .map { kontekst -> kontekst.path("kontekstType").asText() }
    }

    private fun finnVedtaksperiodeId(konteksttyper: List<String>, kontekstdetaljer: List<Map<String, String>>, aktivitet: JsonNode): String? {
        val vedtaksperiodedetaljerindeks = aktivitet
            .path("kontekster")
            .map { it.intValue() }
            .first { indeks -> konteksttyper[indeks] == vedtaksperiodeKontekst }
        return kontekstdetaljer[vedtaksperiodedetaljerindeks][vedtaksperiodeId]
    }
}

