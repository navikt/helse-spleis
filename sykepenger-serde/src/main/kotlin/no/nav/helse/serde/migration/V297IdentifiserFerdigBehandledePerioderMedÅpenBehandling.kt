package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import net.logstash.logback.argument.StructuredArguments.keyValue
import org.slf4j.LoggerFactory

internal class V297IdentifiserFerdigBehandledePerioderMedÅpenBehandling: JsonMigration(version = 297) {
    override val description = "identifisere ferdig behandlede perioder med åpen behandling"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val aktørId = jsonNode.path("aktørId").asText()

        try {
            jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
                val organisasjonsnummer = arbeidsgiver.path("organisasjonsnummer").asText()
                arbeidsgiver.path("vedtaksperioder")
                    .filter { it.path("tilstand").asText() in setOf("AVSLUTTET", "AVSLUTTET_UTEN_UTBETALING") }
                    .forEach { ferdigBehandletVedtaksperiode ->
                        if (ferdigBehandletVedtaksperiode.path("tilstand").asText() == "AVSLUTTET") ferdigBehandletVedtaksperiode.validerVedtakIverksatt(aktørId, organisasjonsnummer)
                        else ferdigBehandletVedtaksperiode.validerAvsluttetUtenVedtak(aktørId, organisasjonsnummer)
                    }
            }
        } catch (exception: Exception) {
            sikkerLogg.error("Feil ved migrering til versjon 297 for {}", keyValue("aktørId", aktørId), exception)
        }
    }

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

        private fun JsonNode.sisteBehandlingOrNull() = path("behandlinger").lastOrNull()

        private fun JsonNode.validerVedtakIverksatt(aktørId: String, organisasjonsnummer: String) {
            val sisteBehandling = sisteBehandlingOrNull() ?: return sikkerLogg.error("Avsluttet {} for {} på {} har ingen behandlinger", keyValue("vedtaksperiodeId", path("id").asText()), keyValue("aktørId", aktørId), keyValue("organisasjonsnummer", organisasjonsnummer))
            val tilstand = sisteBehandling.path("tilstand").asText().takeUnless { it == "VEDTAK_IVERKSATT" } ?: return
            sikkerLogg.error("Avsluttet {} for {} på {} har siste {} i {}", keyValue("vedtaksperiodeId", path("id").asText()), keyValue("aktørId", aktørId), keyValue("organisasjonsnummer", organisasjonsnummer), keyValue("behandlingId", sisteBehandling.path("id").asText()), keyValue("tilstand", tilstand))
        }

        private fun JsonNode.validerAvsluttetUtenVedtak(aktørId: String, organisasjonsnummer: String) {
            val sisteBehandling = sisteBehandlingOrNull() ?: return sikkerLogg.error("AvsluttetUtenUtbetaling {} for {} på {} har ingen behandlinger", keyValue("vedtaksperiodeId", path("id").asText()), keyValue("aktørId", aktørId),  keyValue("organisasjonsnummer", organisasjonsnummer))
            val tilstand = sisteBehandling.path("tilstand").asText().takeUnless { it == "AVSLUTTET_UTEN_VEDTAK" } ?: return
            sikkerLogg.error("AvsluttetUtenUtbetaling {} for {} på {} har siste {} i {}", keyValue("vedtaksperiodeId", path("id").asText()), keyValue("aktørId", aktørId), keyValue("organisasjonsnummer", organisasjonsnummer), keyValue("behandlingId", sisteBehandling.path("id").asText()), keyValue("tilstand", tilstand))
        }
    }
}
