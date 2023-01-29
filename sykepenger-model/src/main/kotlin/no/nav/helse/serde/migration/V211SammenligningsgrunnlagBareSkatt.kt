package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory

internal class V211SammenligningsgrunnlagBareSkatt: JsonMigration(211) {

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    override val description = "Sammenligningsgrunnlaget skal bare ha skatteopplysninger"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("vilkårsgrunnlagHistorikk").forEach { innslag ->
            innslag.path("vilkårsgrunnlag")
                .filter { it.hasNonNull("sammenligningsgrunnlag") }
                .forEach { vilkårsgrunnlag ->
                    val oppdaterte = vilkårsgrunnlag
                        .path("sammenligningsgrunnlag")
                        .path("arbeidsgiverInntektsopplysninger")
                        .deepCopy<JsonNode>()
                        .filter { arbeidsgiverInntektsopplysning ->
                            arbeidsgiverInntektsopplysning.path("inntektsopplysning").hasNonNull("skatteopplysninger")
                        }
                        .onEach { arbeidsgiverInntektsopplysning ->
                            val skatteopplysninger = arbeidsgiverInntektsopplysning.path("inntektsopplysning").path("skatteopplysninger").deepCopy<ArrayNode>()
                            (arbeidsgiverInntektsopplysning as ObjectNode).set<ArrayNode>("skatteopplysninger", skatteopplysninger)
                            arbeidsgiverInntektsopplysning.remove("inntektsopplysning")
                        }

                    (vilkårsgrunnlag.path("sammenligningsgrunnlag") as ObjectNode)
                        .putArray("arbeidsgiverInntektsopplysninger")
                        .apply {
                            addAll(oppdaterte)
                        }
                }
        }
    }

}