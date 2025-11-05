package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.util.UUID

internal class V342ÅrsakTilInnhentingFraAordningen : JsonMigration(342) {
    override val description = "Sette årsakTilInnhenting på inntekter fra Aordningen"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val sykepengegrunnlagForArbeidsgiverHendelseIder by lazy {
            meldingerSupplier.hentMeldinger()
                .mapValues { (_, melding) -> melding.meldingstype }
                .filterValues { meldingstype -> meldingstype == "SYKEPENGEGRUNNLAG_FOR_ARBEIDSGIVER" }
                .keys
        }

        jsonNode.path("vilkårsgrunnlagHistorikk").forEach { historikkInnslag ->
            historikkInnslag.path("vilkårsgrunnlag").forEach { vilkårsgrunnlag ->
                val inntektsgrunnlag = vilkårsgrunnlag.path("inntektsgrunnlag")

                inntektsgrunnlag.path("arbeidsgiverInntektsopplysninger").forEach { arbeidsgiverInntektsopplysning ->
                    val arbeidstakerFaktaavklartInntekt = arbeidsgiverInntektsopplysning.path("inntektsopplysning") as ObjectNode
                    migrerÅrsakTilInnhentingFraAordningenVilkårsgrunnlag(arbeidstakerFaktaavklartInntekt, sykepengegrunnlagForArbeidsgiverHendelseIder)
                }

                inntektsgrunnlag.path("deaktiverteArbeidsforhold").forEach { deaktivert ->
                    val arbeidstakerFaktaavklartInntekt = deaktivert.path("inntektsopplysning") as ObjectNode
                    migrerÅrsakTilInnhentingFraAordningenVilkårsgrunnlag(arbeidstakerFaktaavklartInntekt, sykepengegrunnlagForArbeidsgiverHendelseIder)
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

    // For vilkårsgrunnlag så setter vi verdi ut fra om hendelseId hører til hendelsen SYKEPENGEGRUNNLAG_FOR_ARBEIDSGIVER (IM som aldri kommer)
    private fun migrerÅrsakTilInnhentingFraAordningenVilkårsgrunnlag(arbeidstakerFaktaavklartInntekt: ObjectNode, sykepengegrunnlagForArbeidsgiverHendelseIder: Set<UUID>) {
        if (arbeidstakerFaktaavklartInntekt.path("type").asText() != "ARBEIDSTAKER") return
        if (arbeidstakerFaktaavklartInntekt.path("kilde").asText() != "AORDNINGEN") return
        val hendelseId = arbeidstakerFaktaavklartInntekt.path("hendelseId").asText().uuid
        val årsakTilInnhenting = when (hendelseId in sykepengegrunnlagForArbeidsgiverHendelseIder) {
            true -> "MANGLENDE_INNTEKT_FRA_ARBEIDSGIVER"
            false -> "FASTSETTING_AV_SYKEPENGEGRUNNLAG"
        }
        arbeidstakerFaktaavklartInntekt.put("årsakTilInnhenting", årsakTilInnhenting)
    }

    private fun migrerVedtaksperiode(vedtaksperiode: JsonNode) {
        vedtaksperiode.path("behandlinger").forEach { behandling ->
            behandling.path("endringer").forEach { endring ->
                endring.path("faktaavklartInntekt").takeUnless { it.isMissingNode || it.isNull }?.let { faktaavklartInntekt ->
                    faktaavklartInntekt as ObjectNode
                    migrerÅrsakTilInnhentingFraAordningenVedtaksperiode(faktaavklartInntekt)
                }
            }
        }
    }

    // På behandling skal det ikke være tilfeller av FASTSETTING_AV_SYKEPENGEGRUNNLAG, så her settes alltid MANGLENDE_INNTEKT_FRA_ARBEIDSGIVER
    // FASTSETTING_AV_SYKEPENGEGRUNNLAG er noe som brukes ved ulik-fom & ghost, og er sånn sett ikke behandlingens faktaavklarte inntekt.
    // behandlingens faktaavklarte inntekt vil typisk være inntektsmeldingen som ble valgt bort pga ulik fom
    private fun migrerÅrsakTilInnhentingFraAordningenVedtaksperiode(arbeidstakerFaktaavklartInntekt: ObjectNode) {
        if (arbeidstakerFaktaavklartInntekt.path("type").asText() != "ARBEIDSTAKER") return
        if (arbeidstakerFaktaavklartInntekt.path("kilde").asText() != "AORDNINGEN") return
        arbeidstakerFaktaavklartInntekt.put("årsakTilInnhenting", "MANGLENDE_INNTEKT_FRA_ARBEIDSGIVER")
    }
}
