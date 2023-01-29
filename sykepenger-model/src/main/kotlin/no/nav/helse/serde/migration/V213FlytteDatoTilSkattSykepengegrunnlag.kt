package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate.EPOCH
import no.nav.helse.person.inntekt.SkattSykepengegrunnlag
import org.slf4j.LoggerFactory

internal class V213FlytteDatoTilSkattSykepengegrunnlag: JsonMigration(213) {

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    override val description = "setter dato på SkattSykepengegrunnlag"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("inntektshistorikk").forEach { innslag ->
                innslag.path("inntektsopplysninger")
                    .filter { opplysning -> erSkattSykepengegrunnlag(opplysning) }
                    .forEach { skattSykepengegrunnlag ->
                        migrerSkattSykepengegrunnlag(skattSykepengegrunnlag)
                    }
            }
        }

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
        val dato = skattSykepengegrunnlag.path("skatteopplysninger").firstOrNull()?.path("dato")?.asText() ?: EPOCH.toString()
        (skattSykepengegrunnlag as ObjectNode).put("dato", dato)
    }
}