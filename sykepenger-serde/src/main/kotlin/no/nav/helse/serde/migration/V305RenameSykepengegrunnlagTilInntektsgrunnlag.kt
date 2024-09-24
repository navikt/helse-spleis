package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V305RenameSykepengegrunnlagTilInntektsgrunnlag: JsonMigration(version = 305) {
    override val description = "renamer sykepengegrunnlag til inntektsgrunnlag"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("vilkårsgrunnlagHistorikk").forEach { historikkInnslag ->
            historikkInnslag.path("vilkårsgrunnlag").forEach { vilkårsgrunnlag ->
                vilkårsgrunnlag as ObjectNode
                vilkårsgrunnlag.replace("inntektsgrunnlag", vilkårsgrunnlag.path("sykepengegrunnlag"))
                vilkårsgrunnlag.remove("sykepengegrunnlag")
            }
        }
    }
}