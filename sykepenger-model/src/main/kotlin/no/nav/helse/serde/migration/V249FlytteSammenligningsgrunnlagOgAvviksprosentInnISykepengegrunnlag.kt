package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory

internal class V249FlytteSammenligningsgrunnlagOgAvviksprosentInnISykepengegrunnlag: JsonMigration(249) {
    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    override val description = "flytter sammenligningsgrunnlag og avviksprosent inn i sykepengegrunnlaget"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("vilkårsgrunnlagHistorikk").forEach { innslag ->
            innslag.path("vilkårsgrunnlag").forEach { element ->
                val type = element.path("type").asText()
                val nyttSykepengegrunnlag = element.path("sykepengegrunnlag") as ObjectNode
                if (type == "Vilkårsprøving") {
                    val sammenligningsgrunnlag = element.path("sammenligningsgrunnlag").deepCopy<ObjectNode>()
                    nyttSykepengegrunnlag.replace("sammenligningsgrunnlag", sammenligningsgrunnlag)
                    nyttSykepengegrunnlag.put("vurdertInfotrygd", false)

                    element as ObjectNode
                    element.remove("avviksprosent")
                    element.remove("sammenligningsgrunnlag")
                }
                else {
                    nyttSykepengegrunnlag.put("vurdertInfotrygd", true)
                }
            }
        }

    }
}