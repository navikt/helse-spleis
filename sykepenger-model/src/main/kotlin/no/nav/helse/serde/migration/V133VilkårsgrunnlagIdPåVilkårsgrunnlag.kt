package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.util.*

internal class V133VilkårsgrunnlagIdPåVilkårsgrunnlag : JsonMigration(version = 133) {
    override val description = "Legger til vilkårsgrunnlagId på alle vilkårsgrunnlag"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val vilkårsgrunnlagIder = mutableMapOf<JsonNode, UUID>()
        jsonNode["vilkårsgrunnlagHistorikk"]
            .flatMap { it["vilkårsgrunnlag"] }
            .forEach {
                if (it.hasNonNull("vilkårsgrunnlagId")) return@forEach
                val vilkårsgrunnlagId = vilkårsgrunnlagIder.getOrPut(it.deepCopy()) { UUID.randomUUID() }
                (it as ObjectNode).put("vilkårsgrunnlagId", vilkårsgrunnlagId.toString())
            }
    }
}
