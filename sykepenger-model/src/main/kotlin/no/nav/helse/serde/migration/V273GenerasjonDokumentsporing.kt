package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDateTime
import java.util.UUID
import org.slf4j.LoggerFactory

internal class V273GenerasjonDokumentsporing: JsonMigration(273) {
    override val description = "legger til <dokumentsporing> på generasjon"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val tidligsteTidspunktForHendelse = mutableMapOf<UUID, LocalDateTime>()

        fun oppdaterTidspunkt(hendelse: UUID, tidspunkt: LocalDateTime) {
            tidligsteTidspunktForHendelse.compute(hendelse) { _, gammelVerdi ->
                gammelVerdi?.let { minOf(tidspunkt, gammelVerdi) } ?: tidspunkt
            }
        }

        jsonNode.path("vilkårsgrunnlagHistorikk")
            .reversed()
            .forEach { element ->
                val elementOpprettet = LocalDateTime.parse(element.path("opprettet").asText())
                element.path("vilkårsgrunnlag").forEach { grunnlag ->
                    grunnlag.path("meldingsreferanseId").takeIf(JsonNode::isTextual)?.also { hendelseId ->
                        oppdaterTidspunkt(UUID.fromString(hendelseId.asText()), elementOpprettet)
                    }

                    grunnlag.path("sykepengegrunnlag").path("arbeidsgiverInntektsopplysninger").forEach { opplysning ->
                        opplysning.path("inntektsopplysning").also { inntektopplysning ->
                            val id = UUID.fromString(inntektopplysning.path("hendelseId").asText())
                            val tidspunkt = LocalDateTime.parse(inntektopplysning.path("tidsstempel").asText())
                            oppdaterTidspunkt(id, tidspunkt)
                        }
                        opplysning.path("refusjonsopplysninger").forEach { refusjonsopplysning ->
                            val id = UUID.fromString(refusjonsopplysning.path("meldingsreferanseId").asText())
                            oppdaterTidspunkt(id, elementOpprettet)
                        }
                    }
                }
            }

        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("sykdomshistorikk").forEach { element ->
                val node = element.path("hendelseId")
                if (node.isTextual) {
                    val hendelseId = UUID.fromString(node.asText())
                    val tidspunkt = LocalDateTime.parse(element.path("tidsstempel").asText())
                    oppdaterTidspunkt(hendelseId, tidspunkt)
                }
            }
            arbeidsgiver.path("refusjonshistorikk").forEach { element ->
                val hendelseId = UUID.fromString(element.path("meldingsreferanseId").asText())
                val tidspunkt = LocalDateTime.parse(element.path("tidsstempel").asText())
                oppdaterTidspunkt(hendelseId, tidspunkt)
            }
            arbeidsgiver.path("inntektshistorikk").forEach { element ->
                val hendelseId = UUID.fromString(element.path("hendelseId").asText())
                val tidspunkt = LocalDateTime.parse(element.path("tidsstempel").asText())
                oppdaterTidspunkt(hendelseId, tidspunkt)
            }

            arbeidsgiver.path("vedtaksperioder").forEach { periode -> migrerVedtaksperiode(periode, tidligsteTidspunktForHendelse::get) }
            arbeidsgiver.path("forkastede").forEach { periode -> migrerVedtaksperiode(periode.path("vedtaksperiode"), tidligsteTidspunktForHendelse::get) }
        }
    }

    private fun migrerVedtaksperiode(periode: JsonNode, tidligsteTidspunktForHendelse: (UUID) -> LocalDateTime?) {
        val dokumentsporing = periode.path("hendelseIder").deepCopy<ArrayNode>()
        periode.path("generasjoner").forEach { migrerGenerasjon(it, dokumentsporing, tidligsteTidspunktForHendelse) }
    }

    private fun migrerGenerasjon(generasjon: JsonNode, dokumentsporing: ArrayNode, tidligsteTidspunktForHendelse: (UUID) -> LocalDateTime?) {
        generasjon as ObjectNode
        val generasjonTidspunkt = LocalDateTime.parse(generasjon.path("tidsstempel").asText())
        generasjon.withArray("dokumentsporing").addAll(dokumenterFørGenerasjon(dokumentsporing, generasjonTidspunkt, tidligsteTidspunktForHendelse))
    }

    private fun dokumenterFørGenerasjon(vedtaksperiodeDokumentsporing: ArrayNode, generasjonTidspunkt: LocalDateTime, tidligsteTidspunktForHendelse: (UUID) -> LocalDateTime?): List<JsonNode> {
        return vedtaksperiodeDokumentsporing.filter { sporing ->
            val dokumentId = UUID.fromString(sporing.path("dokumentId").asText())
            val dokumenttype = sporing.path("dokumenttype").asText()
            val tidligsteTidspunkt = checkNotNull(tidligsteTidspunktForHendelse(dokumentId)) {
                "Finner ikke tidligste tidspunkt for hendelse $dokumenttype $dokumentId"
            }
            tidligsteTidspunkt < generasjonTidspunkt
        }
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
