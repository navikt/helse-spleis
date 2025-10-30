package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V340KonsekventNavngivningFaktaavklartInntekt : JsonMigration(340) {
    override val description = "Konsekvent navngivning på faktaavklart inntekt"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("vilkårsgrunnlagHistorikk").forEach { historikkInnslag ->
            historikkInnslag.path("vilkårsgrunnlag").forEach { vilkårsgrunnlag ->
                val inntektsgrunnlag = vilkårsgrunnlag.path("inntektsgrunnlag")
                val selvstendigFaktaavklartInntekt = inntektsgrunnlag.path("selvstendigInntektsopplysninger").path("inntektsopplysning")
                migrerSelvstendigFaktaavklartInntekt(selvstendigFaktaavklartInntekt)

                inntektsgrunnlag.path("arbeidsgiverInntektsopplysninger").forEach { arbeidsgiverInntektsopplysning ->
                    val arbeidstakerFaktaavklartInntekt = arbeidsgiverInntektsopplysning.path("inntektsopplysning") as ObjectNode
                    migrerArbeidstakerFaktaavklartInntekt(arbeidstakerFaktaavklartInntekt)
                }
            }
        }

        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { vedtaksperiode ->
                migrerVedtaksperiode(vedtaksperiode)
            }
            arbeidsgiver.path("forkastede").forEach { forkastet ->
                migrerVedtaksperiode(forkastet.path("vedtaksperiode"))
            }
        }

    }

    private fun migrerVedtaksperiode(vedtaksperiode: JsonNode) {
        vedtaksperiode.path("behandlinger").forEach { behandling ->
            behandling.path("endringer").forEach { endring ->
                endring.path("faktaavklartInntekt").takeUnless { it.isMissingNode || it.isNull }?.let { faktaavklartInntekt ->
                    faktaavklartInntekt as ObjectNode
                    val type = faktaavklartInntekt.path("type").takeUnless { it.isMissingNode || it.isNull }?.asText()
                    when (type) {
                        "SELVSTENDIG_NÆRINGSDRIVENDE",
                        null -> migrerSelvstendigFaktaavklartInntekt(faktaavklartInntekt)
                        else -> migrerArbeidstakerFaktaavklartInntekt(faktaavklartInntekt)
                    }
                }
            }
        }
    }

    private fun migrerSelvstendigFaktaavklartInntekt(selvstendigFaktaavklatInntekt: JsonNode) {
        if (selvstendigFaktaavklatInntekt.isNull || selvstendigFaktaavklatInntekt.isMissingNode) return
        selvstendigFaktaavklatInntekt as ObjectNode
        selvstendigFaktaavklatInntekt.put("type", "SELVSTENDIG") // Fikser null/ SELVSTENDIG_NÆRINGSDRIVENDE
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
