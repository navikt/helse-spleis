package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V92VilkårsvurderingMinimumInntekt : JsonMigration(version = 92) {
    override val description: String = "Legger til minimumInntekt på vilkårsvurderinger for skjæringstidspunkt der vi har avvist dager pga minimum inntekt"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val skjæringstidspunkterUtenMinimumInntekt = jsonNode.path("arbeidsgivere")
            .flatMap { arbeidsgiver -> arbeidsgiver.path("vedtaksperioder") }
            .flatMap { vedtaksperiode -> vedtaksperiode.path("utbetalingstidslinje").path("dager") }
            .filter { it["type"].asText() == "AvvistDag" && it["begrunnelser"].any { begrunnelse -> begrunnelse.asText() == "MinimumInntekt" } }
            .map { it["skjæringstidspunkt"] }
            .distinct()

        val skjæringstidspunkterMedNavDager = jsonNode.path("arbeidsgivere")
            .flatMap { arbeidsgiver -> arbeidsgiver.path("vedtaksperioder") }
            .flatMap { vedtaksperiode -> vedtaksperiode.path("utbetalingstidslinje").path("dager") }
            .filter { it["type"].asText() == "NavDag" }
            .map { it["skjæringstidspunkt"] }
            .distinct()

        val vilkårsgrunnlagHistorikkElementerIkkeOk = jsonNode["vilkårsgrunnlagHistorikk"]
            .filter { it["type"].asText() == "Vilkårsprøving" && it["skjæringstidspunkt"] in skjæringstidspunkterUtenMinimumInntekt }

        val vilkårsgrunnlagHistorikkElementerMinimumInntektOk: List<JsonNode> = jsonNode["vilkårsgrunnlagHistorikk"]
            .filter { it["type"].asText() == "Vilkårsprøving" && it["skjæringstidspunkt"] in skjæringstidspunkterMedNavDager }

        val vilkårsgrunnlagHistorikkElementerMinimumInntektIkkeVurdert = jsonNode["vilkårsgrunnlagHistorikk"]
            .filterNot { it in vilkårsgrunnlagHistorikkElementerIkkeOk || it in vilkårsgrunnlagHistorikkElementerMinimumInntektOk }


        vilkårsgrunnlagHistorikkElementerIkkeOk.forEach {
            (it as ObjectNode).put("harMinimumInntekt", false)
            it.put("vurdertOk", false)
        }
        vilkårsgrunnlagHistorikkElementerMinimumInntektOk.forEach {
            (it as ObjectNode).put("harMinimumInntekt", true)
        }
        vilkårsgrunnlagHistorikkElementerMinimumInntektIkkeVurdert.forEach {
            if (it["type"].asText() == "Vilkårsprøving") {
                (it as ObjectNode).putNull("harMinimumInntekt")
            }
        }

        val vilkårsgrunnlagHistorikk =
            (vilkårsgrunnlagHistorikkElementerMinimumInntektOk + vilkårsgrunnlagHistorikkElementerIkkeOk + vilkårsgrunnlagHistorikkElementerMinimumInntektIkkeVurdert)
                .sortedByDescending { it["skjæringstidspunkt"].asText() }

        jsonNode.putArray("vilkårsgrunnlagHistorikk").addAll(vilkårsgrunnlagHistorikk)
    }


}

