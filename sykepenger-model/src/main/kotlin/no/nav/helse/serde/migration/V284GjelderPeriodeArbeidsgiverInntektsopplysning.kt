package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate

internal class V284GjelderPeriodeArbeidsgiverInntektsopplysning: JsonMigration(284) {
    override val description = "setter fom og tom på arbeidsgiverinntektsopplysning"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        migrer(jsonNode, jsonNode.path("aktørId").asText())
    }

    private fun migrer(jsonNode: ObjectNode, aktørId: String) {
        jsonNode.path("vilkårsgrunnlagHistorikk").forEach { element ->
            element.path("vilkårsgrunnlag")
                .onEach { grunnlag -> migrerSykepengegrunnlag(aktørId, grunnlag) }
        }
    }
    private fun migrerSykepengegrunnlag(aktørId: String, grunnlagsdata: JsonNode) {
        val skjæringstidspunkt = grunnlagsdata.path("skjæringstidspunkt").asText()
        grunnlagsdata.path("sykepengegrunnlag").path("arbeidsgiverInntektsopplysninger").forEach { opplysning ->
            migrerOpplysning(skjæringstidspunkt, opplysning)
        }
        grunnlagsdata.path("sykepengegrunnlag").path("deaktiverteArbeidsforhold").forEach { opplysning ->
            migrerOpplysning(skjæringstidspunkt, opplysning)
        }
    }

    private fun migrerOpplysning(skjæringstidspunkt: String, opplysning: JsonNode) {
        opplysning as ObjectNode
        opplysning.put("fom", skjæringstidspunkt)
        opplysning.put("tom", LocalDate.MAX.toString())
    }
}
