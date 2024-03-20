package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory
import org.slf4j.MDC

internal class V291FikseTidligereOmgjøringerSomErRevurderingFeilet: JsonMigration(version = 291) {
    override val description = "fikser opp i gamle auu-perioder som står i REVURDERING_FEILET, hvor siste behandling er TIL_INFOTRYGD og den nest siste er BEREGNET_OMGJØRING"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val aktørId = jsonNode.path("aktørId").asText()
        val fnr = jsonNode.path("fødselsnummer").asText()
        MDC.putCloseable("aktørId", aktørId).use {
            jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
                val orgnr = arbeidsgiver.path("organisasjonsnummer").asText()
                MDC.putCloseable("orgnr", orgnr).use {
                    arbeidsgiver.path("forkastede")
                        .forEach { forkastet ->
                            migrerVedtaksperiode(forkastet.path("vedtaksperiode"))
                        }
                }
            }
        }
    }

    private fun migrerVedtaksperiode(vedtaksperiode: JsonNode) {
        if (vedtaksperiode.path("tilstand").asText() !in setOf("TIL_INFOTRYGD", "REVURDERING_FEILET")) return
        val generasjoner = vedtaksperiode.path("generasjoner") as ArrayNode
        if (generasjoner.size() < 3) return
        val siste = generasjoner.last()
        if (siste.path("tilstand").asText() != "TIL_INFOTRYGD") return sikkerLogg.info("siste generasjon ${siste.path("id").asText()} står i ${siste.path("tilstand").asText()}")
        val nestSist = generasjoner[generasjoner.size() - 2]
        if (nestSist.path("tilstand").asText() !in setOf("UBEREGNET_OMGJØRING", "BEREGNET_OMGJØRING")) return sikkerLogg.info("nest siste generasjon ${nestSist.path("id").asText()} står i ${nestSist.path("tilstand").asText()}")
        val nestNestSist = generasjoner[generasjoner.size() - 3]
        if (nestNestSist.path("tilstand").asText() != "AVSLUTTET_UTEN_VEDTAK") return sikkerLogg.info("nest-nest siste generasjon ${nestNestSist.path("id").asText()} står i ${nestNestSist.path("tilstand").asText()}")

        val avsluttettidspunkt = siste.path("avsluttet").takeIf(JsonNode::isTextual)?.asText() ?: return sikkerLogg.info("siste generasjon ${siste.path("id").asText()} har ikke avsluttettidspunkt")

        sikkerLogg.info("fjerner siste generasjon ${siste.path("id").asText()} som står i ${siste.path("tilstand").asText()}")

        nestSist as ObjectNode
        nestSist.put("avsluttet", avsluttettidspunkt)
        nestSist.put("tilstand", "TIL_INFOTRYGD")
        generasjoner.remove(generasjoner.size() - 1)
    }

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    }
}