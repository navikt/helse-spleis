package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import net.logstash.logback.argument.StructuredArguments.kv
import org.slf4j.LoggerFactory

internal class V330EpochSomArbeidsgiverperiodeForInfotrygdsaker : JsonMigration(330) {
    override val description = "Legger til EPOCH som Arbeidsgiverperiode på siste behandling på alle Infotrygd-saker"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val fnr = jsonNode.path("fødselsnummer").asText()
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            if (arbeidsgiver.path("yrkesaktivitetstype").asText() == "ARBEIDSTAKER") {
                arbeidsgiver.path("vedtaksperioder").forEach { vedtaksperiode ->
                    migrerVedtaksperiode(vedtaksperiode, fnr)
                }
            }
        }
    }

    private fun migrerVedtaksperiode(vedtaksperiode: JsonNode, fnr: String) {
        val vedtaksperiodeId = vedtaksperiode.path("id").asText()
        val harVærtFattetVedtakPå = vedtaksperiode.path("behandlinger").any { behandling ->
            behandling.path("tilstand").asText() == "VEDTAK_IVERKSATT"
        }

        val sisteBehandling = vedtaksperiode.path("behandlinger").last()
        val sisteEndring = sisteBehandling.path("endringer").last()
        val arbeidsgiverperiode = sisteEndring.path("arbeidsgiverperioder") as ArrayNode

        if (arbeidsgiverperiode.size() > 0) return

        if (!harVærtFattetVedtakPå) {
            if (sisteBehandling.path("tilstand").asText() == "AVSLUTTET_UTEN_VEDTAK") {
                sikkerlogg.info("Tom arbeidsgiverperiode for {} {}", kv("fnr", fnr), kv("vedtaksperiodeId", vedtaksperiodeId))
            }
            return
        }

        val vedtaksperiodeFomÅr = (sisteEndring.path("fom") as ArrayNode)[0].asInt()
        sikkerlogg.info("Lagrer EPOCH som arbeidsgiverperiode for Infotrygd-sak for person {} {} {}", kv("fnr", fnr), kv("vedtaksperiodeId", vedtaksperiodeId), kv("årstall", vedtaksperiodeFomÅr))
        arbeidsgiverperiode.addObject().apply {
            putArray("fom").apply {
                add(1970)
                add(1)
                add(1)
            }
            putArray("tom").apply {
                add(1970)
                add(1)
                add(1)
            }
        }
    }
}

private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
