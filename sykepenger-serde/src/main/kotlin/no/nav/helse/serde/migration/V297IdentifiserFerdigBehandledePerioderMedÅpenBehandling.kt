package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDate
import net.logstash.logback.argument.StructuredArguments.keyValue
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory

internal class V297IdentifiserFerdigBehandledePerioderMedÅpenBehandling: JsonMigration(version = 297) {
    override val description = "identifisere ferdig behandlede perioder med åpen behandling"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val aktørId = jsonNode.path("aktørId").asText()
        val fødselsnummer = jsonNode.path("fødselsnummer").asText()

        try {
            val påminnelser = mutableListOf<Pair<LocalDate, JsonNode>>()

            jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
                val organisasjonsnummer = arbeidsgiver.path("organisasjonsnummer").asText()
                arbeidsgiver.path("vedtaksperioder")
                    .filter { it.path("tilstand").asText() in setOf("AVSLUTTET", "AVSLUTTET_UTEN_UTBETALING") }
                    .forEach { ferdigBehandletVedtaksperiode ->
                        val påminnelse =
                            if (ferdigBehandletVedtaksperiode.path("tilstand").asText() == "AVSLUTTET") ferdigBehandletVedtaksperiode.validerVedtakIverksatt(aktørId, fødselsnummer, organisasjonsnummer)
                            else ferdigBehandletVedtaksperiode.validerAvsluttetUtenVedtak(aktørId, fødselsnummer, organisasjonsnummer)
                        if (påminnelse != null) påminnelser.add(påminnelse)
                    }
            }

            if (påminnelser.isEmpty()) return
            val snutePåminnelse = påminnelser.minBy { it.first }.second
            sikkerLogg.info("Påminnelse for å fikse behandlinger for {}:\n\t${snutePåminnelse}", keyValue("aktørId", aktørId))
        } catch (exception: Exception) {
            sikkerLogg.error("Feil ved migrering til versjon 297 for {}", keyValue("aktørId", aktørId), exception)
        }
    }

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
        private val objectMapper = jacksonObjectMapper()

        private fun JsonNode.sisteBehandlingOrNull() = path("behandlinger").lastOrNull()
        private fun JsonNode.fom() = LocalDate.parse(path("endringer").last().path("fom").asText())

        private fun JsonNode.validerVedtakIverksatt(aktørId: String, fødselsnummer: String, organisasjonsnummer: String): Pair<LocalDate, JsonNode>? {
            val vedtaksperiodeId = path("id").asText()
            val sisteBehandling = sisteBehandlingOrNull() ?: return sikkerLogg.error("Avsluttet {} for {} på {} har ingen behandlinger", keyValue("vedtaksperiodeId", vedtaksperiodeId), keyValue("aktørId", aktørId), keyValue("organisasjonsnummer", organisasjonsnummer)).let { null }
            val tilstand = sisteBehandling.path("tilstand").asText().takeUnless { it == "VEDTAK_IVERKSATT" } ?: return null
            sikkerLogg.error("Avsluttet {} for {} på {} har siste {} i {}", keyValue("vedtaksperiodeId", vedtaksperiodeId), keyValue("aktørId", aktørId), keyValue("organisasjonsnummer", organisasjonsnummer), keyValue("behandlingId", sisteBehandling.path("id").asText()), keyValue("tilstand", tilstand))
            return sisteBehandling.fom() to påminnelse(aktørId, fødselsnummer, organisasjonsnummer, vedtaksperiodeId, "AVSLUTTET")
        }

        private fun JsonNode.validerAvsluttetUtenVedtak(aktørId: String, fødselsnummer: String, organisasjonsnummer: String): Pair<LocalDate, JsonNode>? {
            val vedtaksperiodeId = path("id").asText()
            val sisteBehandling = sisteBehandlingOrNull() ?: return sikkerLogg.error("AvsluttetUtenUtbetaling {} for {} på {} har ingen behandlinger", keyValue("vedtaksperiodeId", vedtaksperiodeId), keyValue("aktørId", aktørId),  keyValue("organisasjonsnummer", organisasjonsnummer)).let { null }
            val tilstand = sisteBehandling.path("tilstand").asText().takeUnless { it == "AVSLUTTET_UTEN_VEDTAK" } ?: return null
            sikkerLogg.error("AvsluttetUtenUtbetaling {} for {} på {} har siste {} i {}", keyValue("vedtaksperiodeId", vedtaksperiodeId), keyValue("aktørId", aktørId), keyValue("organisasjonsnummer", organisasjonsnummer), keyValue("behandlingId", sisteBehandling.path("id").asText()), keyValue("tilstand", tilstand))
            return sisteBehandling.fom() to påminnelse(aktørId, fødselsnummer, organisasjonsnummer, vedtaksperiodeId, "AVSLUTTET_UTEN_UTBETALING")
        }

        @Language("JSON")
        private fun påminnelse(aktørId: String, fødselsnummer: String, organisasjonsnummer: String, vedtaksperiode: String, tilstand: String) = """
            { "@event_name": "påminnelse", "fødselsnummer": "$fødselsnummer", "aktørId": "$aktørId", "organisasjonsnummer": "$organisasjonsnummer", "vedtaksperiodeId": "$vedtaksperiode", "tilstand": "$tilstand", "påminnelsestidspunkt": "{{now}}", "nestePåminnelsestidspunkt": "{{now+1h}}", "tilstandsendringstidspunkt": "{{now-1h}}", "antallGangerPåminnet": 1, "ønskerReberegning": true }
        """.let { objectMapper.readTree(it) }
    }
}
