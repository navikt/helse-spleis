package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.serde.migration.V277UberegnetRevurdering.Dokumentsporing.Companion.dokumentsporing
import no.nav.helse.serde.serdeObjectMapper
import org.slf4j.LoggerFactory

internal class V277UberegnetRevurdering: JsonMigration(277) {
    override val description = "sørger for at vedtaksperiode i AvventerRevurdering har en UberegnetRevurdering-generasjon"

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
        if (tilstandForVedtaksperiode != "AVVENTER_REVURDERING") return
        val generasjoner = periode.path("generasjoner") as ArrayNode
        val sisteGenerasjon = generasjoner.last()
        val tilstandForSisteGenerasjon = sisteGenerasjon.path("tilstand").asText()
        if (tilstandForSisteGenerasjon != "VEDTAK_IVERKSATT") return
        val sisteEndring = sisteGenerasjon.path("endringer").last()
        val sisteDokumentsporing = sisteEndring.path("dokumentsporing").deepCopy<ObjectNode>().dokumentsporing

        val vedtaksperiodeOppdatert = LocalDateTime.parse(periode.path("oppdatert").asText())
        val endring = lagEndring(sisteDokumentsporing.id, sisteDokumentsporing.type, vedtaksperiodeOppdatert, sisteEndring.path("sykmeldingsperiodeFom").asText().dato, sisteEndring.path("sykmeldingsperiodeTom").asText().dato, sisteEndring.path("sykdomstidslinje").deepCopy(), null, null)
        val nyGenerasjon = lagGenerasjon("UBEREGNET_REVURDERING", vedtaksperiodeOppdatert, sisteGenerasjon.path("fom").asText().dato, sisteGenerasjon.path("tom").asText().dato, listOf(endring), null, null)

        sikkerlogg.info("[V277] $aktørId Lager ny UBEREGNET_REVURDERING for ${periode.path("fom").asText()} - ${periode.path("tom").asText()} (${periode.path("id").asText()}) fordi vedtaksperioden er i $tilstandForVedtaksperiode og siste generasjon har tilstand $tilstandForSisteGenerasjon")
        generasjoner.add(nyGenerasjon)
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
