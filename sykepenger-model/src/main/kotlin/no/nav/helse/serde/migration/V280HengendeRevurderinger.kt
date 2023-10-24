package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.serde.migration.V280HengendeRevurderinger.Dokumentsporing.Companion.dokumentsporing
import org.slf4j.LoggerFactory

internal class V280HengendeRevurderinger: JsonMigration(280) {
    override val description = "fjerner hengende uberegnede revurderinger på eldre perioder som er avsluttet"

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
        if (tilstandForVedtaksperiode != "AVSLUTTET") return
        val generasjoner = periode.path("generasjoner") as ArrayNode
        val sisteGenerasjon = generasjoner.last() as ObjectNode
        val tilstandForSisteGenerasjon = sisteGenerasjon.path("tilstand").asText()
        if (tilstandForSisteGenerasjon != "UBEREGNET_REVURDERING") return

        val sisteIverksettingIndex = generasjoner.indexOfLast { it.path("tilstand").asText() == "VEDTAK_IVERKSATT" }
        val sisteIverksetting: JsonNode? = generasjoner.get(sisteIverksettingIndex)
        if (sisteIverksetting !is ObjectNode || sisteIverksettingIndex < 0) {
            sikkerlogg.error("[280] $aktørId Skulle flytte uberegnet revurdering til VEDTAK_IVERKSATT for ${periode.path("fom").asText()} - ${periode.path("tom").asText()} (${periode.path("id").asText()}) fordi vedtaksperioden er i $tilstandForVedtaksperiode og siste generasjon har tilstand $tilstandForSisteGenerasjon, men vedtaksperioden har ikke en generasjon i VEDTAK_IVERKSATT")
            return
        }

        val uberegnede = generasjoner.filterIndexed { index, _ -> index > sisteIverksettingIndex }
        val dokumentsporing = uberegnede
            .flatMap {
                it.path("endringer").map {
                    LocalDateTime.parse(it.path("tidsstempel").asText()) to it.path("dokumentsporing").dokumentsporing
                }
            }.toSet()

        sikkerlogg.info("[280] $aktørId Flytter ${uberegnede.size} generasjoner (${uberegnede.joinToString { it.path("tilstand").asText() }}) til VEDTAK_IVERKSATT (${sisteIverksetting.path("id").asText()}) for ${periode.path("fom").asText()} - ${periode.path("tom").asText()} (${periode.path("id").asText()}) fordi vedtaksperioden er i $tilstandForVedtaksperiode")
        (sisteIverksetting.path("endringer") as ArrayNode).apply {
            val sisteEndring = last()
            dokumentsporing.forEach { (tidsstempel, dokumentsporing) ->
                val kopi = sisteEndring.deepCopy<ObjectNode>()
                kopi.put("id", "${UUID.randomUUID()}")
                kopi.put("tidsstempel", "$tidsstempel")
                (kopi.path("dokumentsporing") as ObjectNode).apply {
                    put("dokumentId", "${dokumentsporing.id}")
                    put("dokumenttype", dokumentsporing.type)
                }
                add(kopi)
            }
        }
        val uberegnedeIder = uberegnede.map { it.path("id").asText().uuid }.toSet()
        generasjoner.removeAll { generasjon ->
            generasjon.path("id").asText().uuid in uberegnedeIder
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
