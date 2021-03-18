package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V84VilkårsvurderingMinimumInntekt : JsonMigration(version = 84) {
    override val description: String = "Legger til minimumInntekt på vilkårsvurderinger for skjæringstidspunkt vi har avvist dager pga minimum inntekt"

    override fun doMigration(jsonNode: ObjectNode) {
        val skjæringstidspunkterUtenMinimumInntekt = jsonNode.path("arbeidsgivere").flatMap { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder")
        }.filter {
            it.path("utbetalingstidslinje").path("dager")
                .any { it["type"].asText() == "AvvistDag" && it["begrunnelse"].asText() == "MinimumInntekt" }
        }
            .map { it["skjæringstidspunkt"] }

        val skjæringstidspunkterMedNavDager = jsonNode.path("arbeidsgivere")
            .flatMap { arbeidsgiver ->
                arbeidsgiver.path("vedtaksperioder")
            }
            .flatMap { vedtaksperiode ->
                vedtaksperiode.path("utbetalingstidslinje").path("dager")
            }.filter {
                it["type"].asText() == "NavDag"
            }
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
            (it as ObjectNode).putNull("harMinimumInntekt")
        }

        val vilkårsgrunnlagHistorikk =
            (vilkårsgrunnlagHistorikkElementerMinimumInntektOk + vilkårsgrunnlagHistorikkElementerIkkeOk + vilkårsgrunnlagHistorikkElementerMinimumInntektIkkeVurdert)
                .sortedByDescending { it["skjæringstidspunkt"].asText() }

        jsonNode.putArray("vilkårsgrunnlagHistorikk").addAll(vilkårsgrunnlagHistorikk)
    }


}

