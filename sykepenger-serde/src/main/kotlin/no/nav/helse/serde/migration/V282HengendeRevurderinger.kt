package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.util.UUID
import org.slf4j.LoggerFactory

internal class V282HengendeRevurderinger: JsonMigration(282) {
    override val description = "fjerner hengende uberegnede revurderinger på perioder som også har en beregnet revurdering"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        migrer(jsonNode, jsonNode.path("aktørId").asText())
    }

    private fun migrer(jsonNode: ObjectNode, aktørId: String) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder")
                .onEach { periode -> migrerVedtaksperiode(aktørId, periode) }
        }
    }
    private fun migrerVedtaksperiode(aktørId: String, periode: JsonNode) {
        val tilstandForVedtaksperiode = periode.path("tilstand").asText()
        if (tilstandForVedtaksperiode !in setOf("AVVENTER_REVURDERING", "AVVENTER_HISTORIKK_REVURDERING", "AVVENTER_SIMULERING_REVURDERING", "AVVENTER_GODKJENNING_REVURDERING")) return
        val generasjoner = periode.path("generasjoner") as ArrayNode
        val sisteGenerasjon = generasjoner.last() as ObjectNode
        val tilstandForSisteGenerasjon = sisteGenerasjon.path("tilstand").asText()
        val åpneGenerasjoner = generasjoner.filter { it.path("tilstand").asText() in setOf("UBEREGNET_REVURDERING", "BEREGNET_REVURDERING") }
        if (åpneGenerasjoner.size == 1) return // alt ok
        if (åpneGenerasjoner.isEmpty()) {
            sikkerlogg.error("[V282] $aktørId Perioden ${periode.path("fom").asText()} - ${periode.path("tom").asText()} (${periode.path("id").asText()}) mangler åpen generasjon! Perioden står i $tilstandForVedtaksperiode og siste generasjon er $tilstandForSisteGenerasjon")
            return
        }

        val fjerne = åpneGenerasjoner.dropLast(1).map { it.path("id").asText().uuid }
        val tidspunktFørste = åpneGenerasjoner.first().path("tidsstempel").asText()
        (åpneGenerasjoner.last() as ObjectNode).also {
            it.put("tidsstempel", tidspunktFørste)
            (it.path("endringer").first() as ObjectNode).put("tidsstempel", tidspunktFørste)
        }

        sikkerlogg.info("[V282] $aktørId Fjerner ${åpneGenerasjoner.size - 1} åpne generasjoner for ${periode.path("fom").asText()} - ${periode.path("tom").asText()} (${periode.path("id").asText()}) i $tilstandForVedtaksperiode")
        generasjoner.removeAll { generasjon ->
            generasjon.path("id").asText().uuid in fjerne
        }
    }

    private data class Dokumentsporing(
        val id: UUID,
        val type: String
    ) {
        companion object {
            val JsonNode.dokumentsporing get() = Dokumentsporing(
                id = this.path("dokumentId").asText().uuid,
                type = this.path("dokumenttype").asText()
            )
        }
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
