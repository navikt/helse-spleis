package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import no.nav.helse.serde.serdeObjectMapper
import org.slf4j.LoggerFactory

internal class V182OverstyrteVilkårsgrunnlag: JsonMigration(182) {
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
                        val skjæringstidspunkt =
                            if (id == "7bdab6ad-3c19-4cd0-9d0d-e0812975d218") {
                                LocalDate.of(2022, 3, 18)
                            } else {
                                LocalDate.parse(grunnlag.path("skjæringstidspunkt").asText())
                            }

                        val deaktiverteOrgnr = grunnlag
                            .path("sykepengegrunnlag")
                            .path("deaktiverteArbeidsforhold")
                            .map { it.asText() }

                        val deaktiverteInntekter = deaktiverteOrgnr
                            .associateWith { orgnr -> inntekter[skjæringstidspunkt]?.inntekter?.get(orgnr).also {
                                if (it == null) {
                                    sikkerlogg.info("Finner ikke orginalinntekt for $skjæringstidspunkt for $orgnr i innslag $innslagIndex for $aktørId")
                                }
                            }}

                        val sp = grunnlag.path("sykepengegrunnlag") as ObjectNode
                        sp.replace("deaktiverteArbeidsforhold", serdeObjectMapper.createArrayNode().addAll(deaktiverteInntekter.mapNotNull {
                            it.value?.deepCopy()
                        }))
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
                                inntekter = grunnlag.path("sykepengegrunnlag").path("arbeidsgiverInntektsopplysninger")
                                    .associateBy { opplysning -> opplysning.path("orgnummer").asText() }
                            )
                        }
                    }
            }
        return inntekterVedFørsteVilkårsprøving
    }

    private class Inntekter(
        val inntekter: Map<String, JsonNode>
    )
}