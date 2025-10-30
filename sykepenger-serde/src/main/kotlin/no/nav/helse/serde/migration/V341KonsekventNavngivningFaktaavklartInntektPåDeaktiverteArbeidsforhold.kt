package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V341KonsekventNavngivningFaktaavklartInntektPåDeaktiverteArbeidsforhold : JsonMigration(341) {
    override val description = "Konsekvent navngivning på faktaavklart inntekt"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("vilkårsgrunnlagHistorikk").forEach { historikkInnslag ->
            historikkInnslag.path("vilkårsgrunnlag").forEach { vilkårsgrunnlag ->
                val inntektsgrunnlag = vilkårsgrunnlag.path("inntektsgrunnlag")

                inntektsgrunnlag.path("deaktiverteArbeidsforhold").forEach { deaktivert ->
                    val arbeidstakerFaktaavklartInntekt = deaktivert.path("inntektsopplysning") as ObjectNode
                    migrerArbeidstakerFaktaavklartInntekt(arbeidstakerFaktaavklartInntekt)
                }
            }
        }
    }

    private fun migrerArbeidstakerFaktaavklartInntekt(arbeidstakerFaktaavklartInntekt: ObjectNode) {
        val type = arbeidstakerFaktaavklartInntekt.path("type").asText()
        val kilde = arbeidstakerFaktaavklartInntekt.path("kilde").asText()
        val riktigKilde = when {
            type == "ARBEIDSTAKER_ARBEIDSGIVER" -> "INNTEKTSMELDING"
            type == "ARBEIDSTAKER_AORDNINGEN" -> "AORDNINGEN"
            kilde == "SKATT_SYKEPENGEGRUNNLAG" -> "AORDNINGEN" // Fikser det sprø navnet SKATT_SYKEPENGEGRUNNLAG
            else -> null // Allerede rett kilde
        }
        riktigKilde?.let { arbeidstakerFaktaavklartInntekt.put("kilde", it) }

        val riktigType = when (type) {
            "ARBEIDSTAKER_ARBEIDSGIVER",
            "ARBEIDSTAKER_AORDNINGEN" -> "ARBEIDSTAKER"
            else -> null // Allerede rett type
        }
        riktigType?.let { arbeidstakerFaktaavklartInntekt.put("type", it) }
    }
}
