package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import net.logstash.logback.argument.StructuredArguments.kv
import org.slf4j.LoggerFactory

internal class V289AvsluttetTidspunktForkastedeGenerasjoner: JsonMigration(version = 289) {
    override val description = "setter avsluttettidspunkt på TIL_INFOTRYGD-generasjoner"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val aktørId = jsonNode.path("aktørId").asText()
        val fnr = jsonNode.path("fødselsnummer").asText()
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val orgnr = arbeidsgiver.path("organisasjonsnummer").asText()
            arbeidsgiver.path("forkastede").forEach { forkastet ->
                migrerVedtaksperiode(fnr, aktørId, orgnr, forkastet.path("vedtaksperiode"))
            }
        }
    }

    private fun migrerVedtaksperiode(fnr: String, aktørId: String, orgnr: String, vedtaksperiode: JsonNode) {
        val generasjonerNode = vedtaksperiode.path("generasjoner") as ArrayNode
        val sisteGenerasjon = generasjonerNode.last() as ObjectNode
        if (sisteGenerasjon.path("tilstand").asText() != "TIL_INFOTRYGD")
            return sikkerLogg.info("[V289] siste generasjon er ikke TIL_INFOTRYGD for ${vedtaksperiode.path("fom").asText()} - ${vedtaksperiode.path("tom").asText()} {}, {}, {}", kv("aktørId", aktørId), kv("orgnr", orgnr), kv("vedtaksperiodeId", vedtaksperiode.path("id").asText()), kv("fødselsnummer", fnr))

        if (sisteGenerasjon.hasNonNull("avsluttet")) return
        sikkerLogg.info("[V289] setter avsluttettidspunkt på generasjon ${sisteGenerasjon.path("id").asText()} for {}", kv("aktørId", aktørId))
        sisteGenerasjon.put("avsluttet", sisteGenerasjon.path("endringer").last().path("tidsstempel").asText())
    }

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    }
}