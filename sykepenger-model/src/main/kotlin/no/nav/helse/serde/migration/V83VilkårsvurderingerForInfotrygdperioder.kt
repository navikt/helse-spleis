package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.serde.serdeObjectMapper

internal class V83VilkårsvurderingerForInfotrygdperioder : JsonMigration(version = 83) {
    override val description: String = "Legger til skjæringstidspunkt på vilkårsvurderinger fra infotrygdperioder"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val originaleHistorikkElementer = jsonNode["vilkårsgrunnlagHistorikk"]
            .filter { it["type"].asText() == "Vilkårsprøving" }

        val vilkårsgrunnlagSkjæringstidspunkter = originaleHistorikkElementer
            .filter { it["vurdertOk"].asBoolean() }
            .map { it["skjæringstidspunkt"].asText() }

        val forkastedeInfotrygdVilkårsgrunnlag = hentInfotrygdVilkårsgrunnlag(jsonNode["arbeidsgivere"]
            .flatMap { arbeidsgiver ->
                arbeidsgiver["forkastede"].map { forkastet -> forkastet["vedtaksperiode"] }
            })

        val infotrygdVilkårsgrunnlag = hentInfotrygdVilkårsgrunnlag(jsonNode["arbeidsgivere"]
            .flatMap { arbeidsgiver ->
                arbeidsgiver["vedtaksperioder"]
            })

        val infotrygdVilkårsgrunnlagHistorikk =
            (forkastedeInfotrygdVilkårsgrunnlag + infotrygdVilkårsgrunnlag - vilkårsgrunnlagSkjæringstidspunkter).sorted().distinct()

        val infotrygdVilkårsgrunnlagNodes = infotrygdVilkårsgrunnlagHistorikk.map {
            serdeObjectMapper.createObjectNode().put("skjæringstidspunkt", it).put("type", "Infotrygd")
        }

        val gammelVilkårsgrunnlagHistorikk = originaleHistorikkElementer
            .filter { it["skjæringstidspunkt"].asText() !in infotrygdVilkårsgrunnlagHistorikk }

        val vilkårsgrunnlagHistorikk = (gammelVilkårsgrunnlagHistorikk + infotrygdVilkårsgrunnlagNodes).sortedByDescending { it["skjæringstidspunkt"].asText() }

        jsonNode.putArray("vilkårsgrunnlagHistorikk").addAll(vilkårsgrunnlagHistorikk)
    }


    private fun hentInfotrygdVilkårsgrunnlag(jsonNode: Iterable<JsonNode>): List<String> =
        jsonNode.filter { it["forlengelseFraInfotrygd"].asText() == "JA" }
            .map { it["skjæringstidspunkt"].asText() }
}

