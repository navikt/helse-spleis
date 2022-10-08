package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import net.logstash.logback.argument.StructuredArguments.keyValue
import org.slf4j.LoggerFactory

internal class V180OverstyrteVilkårsgrunnlag: JsonMigration(180) {
    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
    override val description = "Sporer opp opprinnelig deaktiverte inntekter"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val aktørId = jsonNode.path("aktørId").asText()
        val inntekter = finnInntekterVedVilkårsprøving(jsonNode)
        jsonNode.path("vilkårsgrunnlagHistorikk")
            .forEachIndexed { innslagIndex, innslag ->
                innslag.path("vilkårsgrunnlag")
                    .filter { grunnlag -> grunnlag.path("type").asText() == "Vilkårsprøving" }
                    .filter { grunnlag ->
                        grunnlag.path("sykepengegrunnlag").path("deaktiverteArbeidsforhold").size() > 0
                    }
                    .forEach { grunnlag ->
                        val id = grunnlag.path("vilkårsgrunnlagId").asText()
                        val skjæringstidspunkt = LocalDate.parse(grunnlag.path("skjæringstidspunkt").asText())

                        val deaktiverteOrgnr = grunnlag
                            .path("sykepengegrunnlag")
                            .path("deaktiverteArbeidsforhold")
                            .map { it.asText() }

                        val deaktiverteInntekter = deaktiverteOrgnr
                            .associateWith { orgnr -> inntekter[skjæringstidspunkt]?.inntekter?.get(orgnr) }

                        deaktiverteInntekter
                            .forEach { (orgnr, inntekt) ->
                                if (inntekt == null) sikkerlogg.info("[V180] {} Finner IKKE original inntekt for $orgnr {} {} {}",
                                    keyValue("aktørId", aktørId), keyValue("innslagIndex", innslagIndex), keyValue("id", id), keyValue("skjæringstidspunkt", skjæringstidspunkt))
                                else sikkerlogg.info("[V180] {} Finner original inntekt for $orgnr {} {} {}",
                                    keyValue("aktørId", aktørId), keyValue("innslagIndex", innslagIndex), keyValue("id", id), keyValue("skjæringstidspunkt", skjæringstidspunkt))
                            }
                    }
            }
    }

    private fun finnInntekterVedVilkårsprøving(jsonNode: ObjectNode): Map<LocalDate, Inntekter> {
        val inntekterVedFørsteVilkårsprøving = mutableMapOf<LocalDate, Inntekter>()
        jsonNode.path("vilkårsgrunnlagHistorikk")
            .reversed()
            .forEach { innslag ->
                innslag.path("vilkårsgrunnlag")
                    .filter { grunnlag -> grunnlag.path("type").asText() == "Vilkårsprøving" }
                    .forEach { grunnlag ->
                        val skjæringstidspunkt = LocalDate.parse(grunnlag.path("skjæringstidspunkt").asText())
                        inntekterVedFørsteVilkårsprøving.getOrPut(skjæringstidspunkt) {
                            Inntekter(
                                inntekter = grunnlag.path("sykepengegrunnlag").path("arbeidsgiverInntektsopplysninger").associate { opplysning ->
                                    opplysning.path("orgnummer").asText() to opplysning.path("inntektsopplysning")
                                }
                            )
                        }
                    }
            }
        return inntekterVedFørsteVilkårsprøving
    }
}

private class Inntekter(
    val inntekter: Map<String, JsonNode>
)