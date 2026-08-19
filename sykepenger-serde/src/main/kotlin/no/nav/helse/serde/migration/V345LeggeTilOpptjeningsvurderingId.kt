package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import java.util.UUID

internal class V345LeggeTilOpptjeningsvurderingId : JsonMigration(version = 345) {
    override val description = "Legger til opptjeningsburderingId på alle vilkårsgrunnlag"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("vilkårsgrunnlagHistorikk").forEach { historikkInnslag ->
            historikkInnslag.path("vilkårsgrunnlag").forEach { vilkårsgrunnlag ->
                vilkårsgrunnlag as ObjectNode
                val vilkårsgrunnlagId = vilkårsgrunnlag["vilkårsgrunnlagId"].asText()
                vilkårsgrunnlag.put("opptjeningsvurderingId",
                    UUID.nameUUIDFromBytes("$vilkårsgrunnlagId:Opptjening".toByteArray()).toString())
            }
        }
    }
}
