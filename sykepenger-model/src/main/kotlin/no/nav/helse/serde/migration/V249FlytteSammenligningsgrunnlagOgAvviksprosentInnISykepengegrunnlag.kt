package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
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
                if (type == "Vilkårsprøving") {
                    val avviksprosent = element.path("avviksprosent")
                    val sammenligningsgrunnlag = element.path("sammenligningsgrunnlag")
                    val nyttSykepengegrunnlag = element.path("sykepengegrunnlag").deepCopy<JsonNode>()

                    nyttSykepengegrunnlag as ObjectNode
                    nyttSykepengegrunnlag.replace("avviksprosent", avviksprosent)
                    nyttSykepengegrunnlag.replace("sammenligningsgrunnlag", sammenligningsgrunnlag)

                    element as ObjectNode
                    element.replace("sykepengegrunnlag", nyttSykepengegrunnlag)
                    element.remove("avviksprosent")
                    element.remove("sammenligningsgrunnlag")
                }
            }
        }

    }
}