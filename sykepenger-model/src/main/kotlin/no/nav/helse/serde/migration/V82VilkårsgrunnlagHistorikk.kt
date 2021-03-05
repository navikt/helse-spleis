package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V82VilkårsgrunnlagHistorikk : JsonMigration(version = 82) {
    override val description: String = "Flytter dataForVilkårsvurdering opp til personnivå"
    override fun doMigration(jsonNode: ObjectNode) {
        val vilkårsgrunnlagHistorikk = mutableMapOf<String, JsonNode>()

        hentVilkårsgrunnlag(vilkårsgrunnlagHistorikk, jsonNode["arbeidsgivere"]
            .flatMap { arbeidsgiver ->
                arbeidsgiver["forkastede"].map { forkastet -> forkastet["vedtaksperiode"] }
            })
        hentVilkårsgrunnlag(vilkårsgrunnlagHistorikk, jsonNode["arbeidsgivere"]
            .flatMap{ arbeidsgiver ->
                arbeidsgiver["vedtaksperioder"] }
        )

        jsonNode.putArray("vilkårsgrunnlagHistorikk").addAll(vilkårsgrunnlagHistorikk.values)
    }

    private fun hentVilkårsgrunnlag(vilkårsgrunnlagHistorikk: MutableMap<String, JsonNode>, jsonNode: Iterable<JsonNode>) {
        jsonNode
            .filter { it.hasNonNull("dataForVilkårsvurdering") }
            .forEach {
                it as ObjectNode
                val node = it.remove("dataForVilkårsvurdering") as ObjectNode
                val skjæringstidspunkt = it["skjæringstidspunkt"]
                node.put("type", "Vilkårsprøving")
                node.put("vurdertOk", it["tilstand"].asText() != "TIL_INFOTRYGD")
                node.set<ObjectNode>("skjæringstidspunkt", skjæringstidspunkt)
                node.set<ObjectNode>("sammenligningsgrunnlag", node.remove("beregnetÅrsinntektFraInntektskomponenten"))
                vilkårsgrunnlagHistorikk[skjæringstidspunkt.asText()] = node
            }
        jsonNode.forEach { (it as ObjectNode).remove("dataForVilkårsvurdering") }
    }
}
