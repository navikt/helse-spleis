package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.serde.serdeObjectMapper
import org.slf4j.LoggerFactory

internal class V279AvsluttettidspunktVedtakFattet: JsonMigration(279) {
    override val description = "flytter generasjoner i VEDTAK_FATTET som skulle vært VEDTAK_IVERKSATT"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        migrer(jsonNode, jsonNode.path("aktørId").asText())
    }

    private fun migrer(jsonNode: ObjectNode, aktørId: String) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val avsluttettidspunktForUtbetalinger = arbeidsgiver.path("utbetalinger").associate {
                it.path("id").asText().uuid to it.path("overføringstidspunkt").takeIf(JsonNode::isTextual)?.let { LocalDateTime.parse(it.asText()) }
            }
            arbeidsgiver.path("vedtaksperioder")
                .onEach { periode -> migrerVedtaksperiode(aktørId, periode, avsluttettidspunktForUtbetalinger::getValue) }
        }
    }
    private fun migrerVedtaksperiode(aktørId: String, periode: JsonNode, avsluttettidspunktForUtbetalinger: (UUID) -> LocalDateTime?) {
        val tilstandForVedtaksperiode = periode.path("tilstand").asText()
        if (tilstandForVedtaksperiode != "AVSLUTTET") return
        val generasjoner = periode.path("generasjoner") as ArrayNode
        val sisteGenerasjon = generasjoner.last() as ObjectNode
        val tilstandForSisteGenerasjon = sisteGenerasjon.path("tilstand").asText()
        if (tilstandForSisteGenerasjon != "VEDTAK_FATTET") return

        val utbetalingId = sisteGenerasjon.path("endringer").last().path("utbetalingId").takeIf(JsonNode::isTextual)?.let { UUID.fromString(it.asText()) }
        if (utbetalingId == null) {
            sikkerlogg.error("[V279] $aktørId Skulle flytter generasjon fra $tilstandForSisteGenerasjon til VEDTAK_IVERKSATT for ${periode.path("fom").asText()} - ${periode.path("tom").asText()} (${periode.path("id").asText()}) fordi vedtaksperioden er i $tilstandForVedtaksperiode og siste generasjon har tilstand $tilstandForSisteGenerasjon, men utbetalingId mangler")
            return
        }
        val avsluttettidspunkt = avsluttettidspunktForUtbetalinger(utbetalingId) ?: run {
            sikkerlogg.info("[V279] $aktørId Skulle flytter generasjon fra $tilstandForSisteGenerasjon til VEDTAK_IVERKSATT for ${periode.path("fom").asText()} - ${periode.path("tom").asText()} (${periode.path("id").asText()}) fordi vedtaksperioden er i $tilstandForVedtaksperiode og siste generasjon har tilstand $tilstandForSisteGenerasjon, men avsluttettidspunkt mangler")
            LocalDateTime.parse(periode.path("oppdatert").asText())
        }
        sikkerlogg.info("[V279] $aktørId Flytter generasjon fra $tilstandForSisteGenerasjon til VEDTAK_IVERKSATT for ${periode.path("fom").asText()} - ${periode.path("tom").asText()} (${periode.path("id").asText()}) fordi vedtaksperioden er i $tilstandForVedtaksperiode og siste generasjon har tilstand $tilstandForSisteGenerasjon")
        sisteGenerasjon.put("tilstand", "VEDTAK_IVERKSATT")
        sisteGenerasjon.put("avsluttet", "$avsluttettidspunkt")
    }

    private fun lagGenerasjon(starttilstand: String, opprettettidspunkt: LocalDateTime, periodeFom: LocalDate, periodeTom: LocalDate, endringer: List<ObjectNode>, vedtakFattet: LocalDateTime? = null, avsluttet: LocalDateTime? = null): ObjectNode {
        return serdeObjectMapper
            .createObjectNode()
            .put("id", "${UUID.randomUUID()}")
            .put("tidsstempel", "$opprettettidspunkt")
            .put("tilstand", starttilstand)
            .put("fom", "$periodeFom")
            .put("tom", "$periodeTom")
            .also {
                if (vedtakFattet == null) it.putNull("vedtakFattet")
                else it.put("vedtakFattet", "$vedtakFattet")
            }
            .also {
                if (avsluttet == null) it.putNull("avsluttet")
                else it.put("avsluttet", "$avsluttet")
            }
            .also {
                it.putArray("endringer").addAll(endringer)
            }
    }
    private fun lagEndring(
        dokumentId: UUID,
        dokumenttype: String,
        opprettettidspunkt: LocalDateTime,
        sykmeldingsperiodeFom: LocalDate,
        sykmeldingsperiodeTom: LocalDate,
        sykdomstidslinje: ObjectNode,
        forkastetUtbetalingId: UUID?,
        forkastetVilkårsgrunnlagId: UUID?
    ): ObjectNode {
        return serdeObjectMapper
            .createObjectNode()
            .put("id", "${UUID.randomUUID()}")
            .put("tidsstempel", "$opprettettidspunkt")
            .put("sykmeldingsperiodeFom", "$sykmeldingsperiodeFom")
            .put("sykmeldingsperiodeTom", "$sykmeldingsperiodeTom")
            .put("fom", sykdomstidslinje.path("periode").path("fom").asText())
            .put("tom", sykdomstidslinje.path("periode").path("tom").asText())
            .also {
                if (forkastetUtbetalingId == null) it.putNull("utbetalingId")
                else it.put("utbetalingId", "$forkastetUtbetalingId")
            }
            .also {
                if (forkastetVilkårsgrunnlagId == null) it.putNull("vilkårsgrunnlagId")
                else it.put("vilkårsgrunnlagId", "$forkastetVilkårsgrunnlagId")
            }
            .also { endringobj ->
                endringobj.set<ObjectNode>("sykdomstidslinje", sykdomstidslinje)
            }
            .also { endringobj ->
                endringobj
                    .putObject("dokumentsporing")
                    .put("dokumentId", "$dokumentId")
                    .put("dokumenttype", dokumenttype)
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
