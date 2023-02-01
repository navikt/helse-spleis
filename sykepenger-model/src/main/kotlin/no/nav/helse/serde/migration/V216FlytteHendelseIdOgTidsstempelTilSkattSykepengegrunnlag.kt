package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory

internal class V216FlytteHendelseIdOgTidsstempelTilSkattSykepengegrunnlag: JsonMigration(216) {

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    override val description = "setter hendelseId og tidsstempel SkattSykepengegrunnlag"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("vilkårsgrunnlagHistorikk").forEach { innslag ->
            innslag.path("vilkårsgrunnlag").forEach { vilkårsgrunnlag ->
                vilkårsgrunnlag.path("sykepengegrunnlag")
                    .path("arbeidsgiverInntektsopplysninger")
                    .filter { opplysning -> erSkattSykepengegrunnlag(opplysning.path("inntektsopplysning")) }
                    .forEach { opplysning -> migrerSkattSykepengegrunnlag(opplysning.path("inntektsopplysning")) }
                vilkårsgrunnlag.path("sykepengegrunnlag")
                    .path("deaktiverteArbeidsforhold")
                    .filter { opplysning -> erSkattSykepengegrunnlag(opplysning.path("inntektsopplysning")) }
                    .forEach { opplysning -> migrerSkattSykepengegrunnlag(opplysning.path("inntektsopplysning")) }
            }
        }
    }

    private fun erSkattSykepengegrunnlag(opplysning: JsonNode) =
        opplysning.path("skatteopplysninger").any { skatteopplysning ->
            skatteopplysning.path("kilde").asText() == "SKATT_SYKEPENGEGRUNNLAG"
        }

    private fun migrerSkattSykepengegrunnlag(skattSykepengegrunnlag: JsonNode) {
        val tidsstempel = skattSykepengegrunnlag.path("skatteopplysninger").first().path("tidsstempel").asText()
        val hendelseId = skattSykepengegrunnlag.path("skatteopplysninger").first().path("hendelseId").asText()
        (skattSykepengegrunnlag as ObjectNode).put("tidsstempel", tidsstempel)
        skattSykepengegrunnlag.put("hendelseId", hendelseId)
        skattSykepengegrunnlag.put("kilde", "SKATT_SYKEPENGEGRUNNLAG")
    }
}