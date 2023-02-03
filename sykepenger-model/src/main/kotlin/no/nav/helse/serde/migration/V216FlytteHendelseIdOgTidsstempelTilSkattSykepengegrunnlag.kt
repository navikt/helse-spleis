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
            val innslagOpprettet = innslag.path("opprettet").asText()
            innslag.path("vilkårsgrunnlag").forEach { vilkårsgrunnlag ->
                val meldingsreferanseId = vilkårsgrunnlag.path("meldingsreferanseId").asText()
                vilkårsgrunnlag.path("sykepengegrunnlag")
                    .path("arbeidsgiverInntektsopplysninger")
                    .filter { opplysning -> erSkattSykepengegrunnlag(opplysning.path("inntektsopplysning")) }
                    .forEach { opplysning -> migrerSkattSykepengegrunnlag(opplysning.path("inntektsopplysning"), innslagOpprettet, meldingsreferanseId) }
                vilkårsgrunnlag.path("sykepengegrunnlag")
                    .path("deaktiverteArbeidsforhold")
                    .filter { opplysning -> erSkattSykepengegrunnlag(opplysning.path("inntektsopplysning")) }
                    .forEach { opplysning -> migrerSkattSykepengegrunnlag(opplysning.path("inntektsopplysning"), innslagOpprettet, meldingsreferanseId) }
            }
        }
    }

    private fun erSkattSykepengegrunnlag(opplysning: JsonNode) =
        opplysning.hasNonNull("skatteopplysninger")

    private fun migrerSkattSykepengegrunnlag(skattSykepengegrunnlag: JsonNode, innslagOpprettet: String, hendelseId: String) {
        val tidsstempel = skattSykepengegrunnlag.path("skatteopplysninger").firstOrNull()?.path("tidsstempel")?.asText() ?: innslagOpprettet
        val hendelseId = skattSykepengegrunnlag.path("skatteopplysninger").firstOrNull()?.path("hendelseId")?.asText() ?: hendelseId
        (skattSykepengegrunnlag as ObjectNode).put("tidsstempel", tidsstempel)
        skattSykepengegrunnlag.put("hendelseId", hendelseId)
        skattSykepengegrunnlag.put("kilde", "SKATT_SYKEPENGEGRUNNLAG")
    }
}