package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import java.util.UUID

internal class V348LeggeTilMedlemskapsvurderingId : JsonMigration(version = 348) {
    override val description = "Legger til medlemskapsvurderingId på alle vilkårsgrunnlag"

    override fun doMigration(
        jsonNode: ObjectNode,
        meldingerSupplier: MeldingerSupplier,
    ) {
        jsonNode.path("vilkårsgrunnlagHistorikk").forEach { historikkInnslag ->
            historikkInnslag.path("vilkårsgrunnlag").forEach { vilkårsgrunnlag ->
                vilkårsgrunnlag as ObjectNode
                val vilkårsgrunnlagId = vilkårsgrunnlag["vilkårsgrunnlagId"].asText()
                vilkårsgrunnlag.put(
                    "medlemskapsvurderingId",
                    UUID.nameUUIDFromBytes("$vilkårsgrunnlagId:Medlemskap".toByteArray()).toString(),
                )
            }
        }
    }
}
