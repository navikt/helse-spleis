package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.serde.serdeObjectMapper
import org.slf4j.LoggerFactory

internal class V281ForkasteAvsluttedePerioderMedUberegnetGenerasjon: JsonMigration(281) {
    override val description = "forkaster vedtaksperioder som er Avsluttet, men som har én generasjon som er UBEREGNET med forkastet utbetaling"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        migrer(jsonNode, jsonNode.path("aktørId").asText())
    }

    private fun migrer(jsonNode: ObjectNode, aktørId: String) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val forkastede = arbeidsgiver.path("forkastede") as ArrayNode
            val forkastinger = mutableListOf<JsonNode>()
            fun nyForkasting(periode: JsonNode) {
                periode as ObjectNode
                periode.put("tilstand", "TIL_INFOTRYGD")
                (periode.path("generasjoner").last() as ObjectNode).also {
                    it.put("tilstand", "TIL_INFOTRYGD")
                    it.put("avsluttet", periode.path("oppdatert").asText())
                }
                forkastinger.add(periode)
            }

            val vedtaksperioder = arbeidsgiver.path("vedtaksperioder") as ArrayNode
            vedtaksperioder.onEach { periode -> migrerVedtaksperiode(aktørId, periode, ::nyForkasting) }

            forkastinger.forEach { periode ->
                val forkastetPeriode = serdeObjectMapper.createObjectNode().apply {
                    set<ObjectNode>("vedtaksperiode", periode)
                }
                forkastede.add(forkastetPeriode)
                val indeks = vedtaksperioder.indexOfFirst { vedtaksperiode -> vedtaksperiode.path("id").asText().uuid == periode.path("id").asText().uuid }
                vedtaksperioder.remove(indeks)
            }
        }
    }
    private fun migrerVedtaksperiode(aktørId: String, periode: JsonNode, leggTilForkastet: (JsonNode) -> Unit) {
        val tilstandForVedtaksperiode = periode.path("tilstand").asText()
        if (tilstandForVedtaksperiode != "AVSLUTTET") return
        val generasjoner = periode.path("generasjoner") as ArrayNode
        val sisteGenerasjon = generasjoner.last() as ObjectNode
        val tilstandForSisteGenerasjon = sisteGenerasjon.path("tilstand").asText()
        if (tilstandForSisteGenerasjon != "UBEREGNET") return

        val sisteIverksettingIndex = generasjoner.indexOfLast { it.path("tilstand").asText() == "VEDTAK_IVERKSATT" }
        val sisteIverksetting: JsonNode? = generasjoner.get(sisteIverksettingIndex)
        if (sisteIverksetting is ObjectNode) {
            sikkerlogg.info("[V281] $aktørId Har UBEREGNET generasjon etter en VEDTAK_IVERKSATT ${periode.path("fom").asText()} - ${periode.path("tom").asText()} (${periode.path("id").asText()}) og vedtaksperioden er $tilstandForVedtaksperiode")
            return
        }

        sikkerlogg.info("[V281] $aktørId Forkaster vedtaksperiode som er i AVSLUTTET, men med FORKASTET utbetaling og er UBEREGNET ${periode.path("fom").asText()} - ${periode.path("tom").asText()} (${periode.path("id").asText()})")
        leggTilForkastet(periode)
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
