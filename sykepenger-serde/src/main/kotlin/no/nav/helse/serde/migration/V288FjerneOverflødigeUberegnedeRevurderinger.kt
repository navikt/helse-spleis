package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory

internal class V288FjerneOverflødigeUberegnedeRevurderinger: JsonMigration(version = 288) {
    override val description = "smelter sammen flere uberegnede (revurderinger) til én"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val aktørId = jsonNode.path("aktørId").asText()
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val orgnr = arbeidsgiver.path("organisasjonsnummer").asText()
            arbeidsgiver.path("forkastede").forEach { forkastet ->
                migrerUberegnedeRevurderinger(aktørId, orgnr, forkastet.path("vedtaksperiode"))
                migrerUberegnedeOmgjøringer(aktørId, orgnr, forkastet.path("vedtaksperiode"))
            }
        }
    }

    private fun migrerUberegnedeRevurderinger(aktørId: String, orgnr: String, vedtaksperiode: JsonNode) {
        val generasjonerNode = vedtaksperiode.path("generasjoner") as ArrayNode
        val generasjoner = generasjonerNode.toList()
        if (generasjoner.size < 2) return
        val uberegnede = generasjoner.dropLast(1).takeLastWhile {
            it.path("tilstand").asText() == "UBEREGNET_REVURDERING"
        }
        smeltSammen(aktørId, generasjonerNode, generasjoner, uberegnede)
    }

    private fun migrerUberegnedeOmgjøringer(aktørId: String, orgnr: String, vedtaksperiode: JsonNode) {
        val generasjonerNode = vedtaksperiode.path("generasjoner") as ArrayNode
        val generasjoner = generasjonerNode.toList()
        if (generasjoner.size < 2) return
        (generasjonerNode.first() as ObjectNode).apply {
            if (path("tilstand").asText() == "UBEREGNET") {
                put("tilstand", "AVSLUTTET_UTEN_VEDTAK")
                val sisteEndring = path("endringer").last().path("tidsstempel").asText()
                put("avsluttet", sisteEndring)
            }
        }
        val uberegnede = generasjoner.drop(1).dropLast(1).takeLastWhile {
            it.path("tilstand").asText() == "UBEREGNET"
        }
        smeltSammen(aktørId, generasjonerNode, generasjoner, uberegnede)
    }

    private fun smeltSammen(aktørId: String, generasjonerNode: ArrayNode, generasjoner: List<JsonNode>, uberegnede: List<JsonNode>) {
        if (uberegnede.isEmpty()) return
        val sisteGenerasjon = generasjoner.last()
        check(sisteGenerasjon.path("tilstand").asText() == "TIL_INFOTRYGD") {
            "Den siste generasjonen er ikke i TIL_INFOTRYGD for aktørId=$aktørId generasjonId=${sisteGenerasjon.path("id").asText()}"
        }
        val avsluttettidspunkt = checkNotNull(sisteGenerasjon.path("avsluttet").asText().takeUnless(String::isBlank)) {
            "Ingen avsluttetidspunkt på generasjon ${sisteGenerasjon.path("id").asText()} for aktørId=$aktørId"
        }
        (uberegnede.first() as ObjectNode).apply {
            put("tilstand", "TIL_INFOTRYGD")
            put("avsluttet", avsluttettidspunkt)
        }
        sikkerLogg.info("fjerner ${uberegnede.size} generasjoner")
        repeat(uberegnede.size) {
            generasjonerNode.remove(generasjonerNode.size() - 1)
        }
    }

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    }
}