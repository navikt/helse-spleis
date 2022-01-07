package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V138SammenligningsgrunnlagPerArbeidsgiverIVilkårsgrunnlag : JsonMigration(version = 138) {
    override val description = "Migrer inn sammenligningsgrunnlag per arbeidsgiver i vilkårsgrunnlaget"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val sammenligningsgrunnlagFraHistorikk = jsonNode["arbeidsgivere"]
            .map { arbeidsgiver ->
                arbeidsgiver["organisasjonsnummer"].asText() to arbeidsgiver["inntektshistorikk"]
                    .flatMap { it["inntektsopplysninger"] }
                    .filter { it.hasNonNull("skatteopplysninger") }
                    .filter { it["skatteopplysninger"].first()["kilde"].asText() == "SKATT_SAMMENLIGNINGSGRUNNLAG" }
            }

        jsonNode["vilkårsgrunnlagHistorikk"]
            .flatMap { it["vilkårsgrunnlag"] }
            .filter { it["type"].asText() == "Vilkårsprøving" }
            .map { it as ObjectNode }
            .forEach { vilkårsgrunnlag ->
                val skjæringstidspunkt = vilkårsgrunnlag["skjæringstidspunkt"].asText()
                val sammenligningsgrunnlagBeløp = vilkårsgrunnlag.remove("sammenligningsgrunnlag")
                val sammenligningsgrunnlag = vilkårsgrunnlag.putObject("sammenligningsgrunnlag")
                sammenligningsgrunnlag.set<ObjectNode>("sammenligningsgrunnlag", sammenligningsgrunnlagBeløp)
                val inntektsopplysninger = sammenligningsgrunnlag.putArray("arbeidsgiverInntektsopplysninger")
                sammenligningsgrunnlagFraHistorikk.forEach { (orgnummer, sammenligningsgrunnlagOpplysning) ->
                    val relevantSammenligningsgrunnlag = sammenligningsgrunnlagOpplysning
                        .firstOrNull {
                            it["skatteopplysninger"].first()["dato"].asText() == skjæringstidspunkt
                        }
                    if (relevantSammenligningsgrunnlag != null) {
                        inntektsopplysninger.addObject()
                            .put("orgnummer", orgnummer)
                            .set<ObjectNode>("inntektsopplysning", relevantSammenligningsgrunnlag.deepCopy())
                    }
                }
            }
    }

}
