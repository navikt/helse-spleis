package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.serde.serdeObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class V320ManglendeRefusjonsopplysninger: JsonMigration(version = 320) {
    override val description = "Legger til full refusjon på utbetalte perioder som mangler refusjonsopplysninger på behandlingen"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val vilkårsgrunnlag = jsonNode
            .path("vilkårsgrunnlagHistorikk")
            .flatMap { it.path("vilkårsgrunnlag") }
            .reversed() // pga associateBy: If any two elements would have the same key returned by keySelector the last one gets added to the map.
            .associateBy { it.path("vilkårsgrunnlagId").asText() }

        val fødselsnummer = jsonNode.path("fødselsnummer").asText()

        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val organisasjonsnummer = arbeidsgiver.path("organisasjonsnummer").asText()

            arbeidsgiver.path("vedtaksperioder").forEach vedtaksperiode@ { vedtaksperiode ->
                val vedtaksperiodeId = vedtaksperiode.path("id").asText()

                val sisteBehandling = vedtaksperiode.path("behandlinger").lastOrNull() ?: return@vedtaksperiode
                if (sisteBehandling.path("tilstand").asText() != "VEDTAK_IVERKSATT") return@vedtaksperiode // Kun aktuelt for iverksatt vedtak
                val behandlingId = sisteBehandling.path("id").asText()

                val sisteEndring = sisteBehandling.path("endringer")?.lastOrNull() ?: return@vedtaksperiode
                val gjeldendeRefusjonstidslinje = sisteEndring.path("refusjonstidslinje") as ObjectNode
                if (gjeldendeRefusjonstidslinje.path("perioder").harRefusjonsopplysninger) return@vedtaksperiode // Da er det jo greit

                // Nå er vi en iverksatt behandling uten refusjonsopplysninger
                val fullRefusjon = vilkårsgrunnlag[sisteEndring.path("vilkårsgrunnlagId").asText()]
                    ?.path("inntektsgrunnlag")
                    ?.path("arbeidsgiverInntektsopplysninger")
                    ?.firstOrNull { it.path("orgnummer").asText() == organisasjonsnummer }
                    ?.fullRefusjon
                    ?: return@vedtaksperiode sikkerlogg.warn("Mangler fortsatt refusjonsopplysninger på {}, {} for {} i {}",
                        keyValue("vedtaksperiodeId", vedtaksperiodeId),
                        keyValue("behandlingId", behandlingId),
                        keyValue("fødselsnummer", fødselsnummer),
                        keyValue("år", "${LocalDate.parse(sisteEndring.path("fom").asText()).year}")
                    )

                // Nå har vi det vi trenger til å klaske det inn
                sikkerlogg.info("Legger til full refusjon på {}, {} for {} i {}",
                    keyValue("vedtaksperiodeId", vedtaksperiodeId),
                    keyValue("behandlingId", behandlingId),
                    keyValue("fødselsnummer", fødselsnummer),
                    keyValue("år", "${LocalDate.parse(sisteEndring.path("fom").asText()).year}")
                )

                gjeldendeRefusjonstidslinje.replace("perioder", serdeObjectMapper.createArrayNode().apply {
                    add(serdeObjectMapper.createObjectNode().apply {
                        put("fom", sisteEndring.path("fom").asText())
                        put("tom", sisteEndring.path("tom").asText())
                        put("meldingsreferanseId", fullRefusjon.meldingsreferanseId)
                        put("avsender", fullRefusjon.avsender)
                        put("tidsstempel", fullRefusjon.tidsstempel)
                        put("dagligBeløp", fullRefusjon.dagligBeløp)
                    })
                })
            }
        }
    }

    private companion object {
        private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")

        private val JsonNode.harRefusjonsopplysninger get() = isArray && !isEmpty

        private data class FullRefusjon(private val månedligBeløp: Double, val tidsstempel: String, val meldingsreferanseId: String, val avsender: String) {
            val dagligBeløp = (månedligBeløp * 12) / 260
        }

        private val JsonNode.fullRefusjon get() = when {
            !path("skjønnsmessigFastsatt").isNull -> path("skjønnsmessigFastsatt").let {
                FullRefusjon(månedligBeløp = it.path("beløp").asDouble(), tidsstempel = it.path("tidsstempel").asText(), meldingsreferanseId = it.path("hendelseId").asText(), avsender = "SAKSBEHANDLER")
            }
            !path("korrigertInntekt").isNull -> path("korrigertInntekt").let {
                FullRefusjon(månedligBeløp = it.path("beløp").asDouble(), tidsstempel = it.path("tidsstempel").asText(), meldingsreferanseId = it.path("hendelseId").asText(), avsender = "SAKSBEHANDLER")
            }
            else -> path("inntektsopplysning").let {
                FullRefusjon(månedligBeløp = it.path("beløp").asDouble(), tidsstempel = it.path("tidsstempel").asText(), meldingsreferanseId = it.path("hendelseId").asText(), avsender = "ARBEIDSGIVER")
            }
        }
    }
}
