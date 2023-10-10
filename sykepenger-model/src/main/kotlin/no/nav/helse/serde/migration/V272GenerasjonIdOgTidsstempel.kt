package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDateTime
import java.util.UUID
import org.slf4j.LoggerFactory

internal class V272GenerasjonIdOgTidsstempel: JsonMigration(272) {
    override val description = "legger til <id> og <tidsstempel> på generasjon"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val tidsstempelFraUtbetaling = arbeidsgiver.path("utbetalinger").associate { utbetaling ->
                utbetaling.path("id").asText() to LocalDateTime.parse(utbetaling.path("tidsstempel").asText())
            }
            arbeidsgiver.path("vedtaksperioder").forEach { periode -> migrerVedtaksperiode(periode, tidsstempelFraUtbetaling::getValue) }
            arbeidsgiver.path("forkastede").forEach { periode -> migrerVedtaksperiode(periode.path("vedtaksperiode"), tidsstempelFraUtbetaling::getValue) }
        }
    }

    private fun migrerVedtaksperiode(periode: JsonNode, tidsstempelFraUtbetaling: (String) -> LocalDateTime) {
        periode.path("generasjoner").forEach { migrerGenerasjon(it, tidsstempelFraUtbetaling) }
    }

    private fun migrerGenerasjon(generasjon: JsonNode, tidsstempelFraUtbetaling: (String) -> LocalDateTime) {
        generasjon as ObjectNode
        val utbetalingId = generasjon.path("utbetalingId").asText()
        generasjon.put("id", "${UUID.randomUUID()}")
        generasjon.put("tidsstempel", tidsstempelFraUtbetaling(utbetalingId).toString())
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
