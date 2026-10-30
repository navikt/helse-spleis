package no.nav.helse.serde.migration

import tools.jackson.databind.node.ObjectNode
import java.util.*

internal class V349LeggeTilRettighetskategoriId : JsonMigration(version = 349) {
    override val description = "Legger til rettighetskategoriId på alle vilkårsgrunnlag"

    override fun doMigration(
        jsonNode: ObjectNode,
        meldingerSupplier: MeldingerSupplier,
    ) {
        jsonNode.path("vilkårsgrunnlagHistorikk").forEach { historikkInnslag ->
            historikkInnslag.path("vilkårsgrunnlag").forEach { vilkårsgrunnlag ->
                vilkårsgrunnlag as ObjectNode
                val vilkårsgrunnlagId = vilkårsgrunnlag["vilkårsgrunnlagId"].asText()
                vilkårsgrunnlag.put(
                    "rettighetskategoriId",
                    UUID.nameUUIDFromBytes("$vilkårsgrunnlagId:Rettighetskategori".toByteArray()).toString(),
                )
            }
        }
    }
}
