package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.forrigeDag
import no.nav.helse.hendelser.til
import no.nav.helse.serde.serdeObjectMapper
import org.slf4j.LoggerFactory

internal class V199InfotrygdDefaultRefusjon: JsonMigration(version = 199) {
    override val description = """Defaultrefusjonsopplysninger for Infotrygdperioder"""

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val aktørId = jsonNode.path("aktørId").asText()

        jsonNode.path("vilkårsgrunnlagHistorikk").firstOrNull()?.also { innslag ->
            innslag.path("vilkårsgrunnlag")
                .filter { element ->
                    element.path("type").asText() == "Infotrygd"
                }
                .forEach { element ->
                    element.path("sykepengegrunnlag").path("arbeidsgiverInntektsopplysninger").forEach { opplysning ->
                        val inntektsopplysning = opplysning.path("inntektsopplysning")
                        val dato = LocalDate.parse(inntektsopplysning.path("dato").asText())
                        val refusjonsopplysninger = opplysning.path("refusjonsopplysninger") as ArrayNode

                        val refusjonsopplysningPåInntektdato = refusjonsopplysninger.firstOrNull {
                            val fom = LocalDate.parse(it.path("fom").asText())
                            val tom = it.path("tom").takeIf(JsonNode::isTextual)?.asText()?.let { LocalDate.parse(it) } ?: LocalDate.MAX
                            dato in (fom til tom)
                        }

                        if (refusjonsopplysningPåInntektdato == null) {
                            val tom = refusjonsopplysninger.firstOrNull {
                                val fom = LocalDate.parse(it.path("fom").asText())
                                fom > dato
                            }?.let { LocalDate.parse(it.path("fom").asText()).forrigeDag }
                            refusjonsopplysninger.add(serdeObjectMapper.createObjectNode().apply {
                                put("meldingsreferanseId", inntektsopplysning.path("hendelseId").asText())
                                put("fom", dato.toString())
                                if (tom != null) put("tom", tom.toString()) else putNull("tom")
                                put("beløp", inntektsopplysning.path("beløp").asDouble())
                            })

                            sikkerLogg.info("Legger til refusjonsopplysning $dato-$tom for {}", keyValue("aktørId", aktørId))
                        }
                    }
                }
        }
    }

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }
}