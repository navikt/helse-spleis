package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import net.logstash.logback.argument.StructuredArguments.kv
import org.slf4j.LoggerFactory

internal class V287AnnullerteÅpneRevurderingerEnGangTil: JsonMigration(version = 287) {
    override val description = "smelter sammen til_infotrygd-generasjonen og nest siste generasjon hvis den er en åpen revurdering"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("forkastede").forEach { forkastet ->
                migrerForkastetVedtaksperiode("", "", forkastet.path("vedtaksperiode"))
            }
        }
    }

    private fun migrerForkastetVedtaksperiode(aktørId: String, orgnr: String, vedtaksperiode: JsonNode) {
        val generasjonerNode = vedtaksperiode.path("generasjoner") as ArrayNode
        val generasjoner = generasjonerNode.toList()
        if (generasjoner.size < 2) return
        val deToSiste = generasjoner.takeLast(2)
        val nestSist = deToSiste.first() as ObjectNode
        val sisteGenerasjon = deToSiste.last()
        if (nestSist.path("tilstand").asText() !in setOf("UBEREGNET_REVURDERING", "BEREGNET_REVURDERING")) return
        nestSist.put("tilstand", "TIL_INFOTRYGD")
        val avsluttettidspunkt = sisteGenerasjon.path("avsluttet").asText().takeUnless(String::isBlank)
        if (avsluttettidspunkt == null) return sikkerLogg.info("[V287] Ingen avsluttettidspunkt for generasjon ${sisteGenerasjon.path("id").asText()}", kv("aktørId", aktørId), kv("orgnr", orgnr))
        nestSist.put("avsluttet", avsluttettidspunkt)
        generasjonerNode.remove(generasjoner.lastIndex)
    }

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    }
}