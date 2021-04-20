package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode


internal class V2Medlemskapstatus : JsonMigration(version = 2) {

    override val description = "Utvider Grunnlagsdata med medlemskapstatus"

    private val medlemskapstatusKey = "medlemskapstatus"
    private val vetIkke = "VET_IKKE"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { periode ->
                migrerVilkårsvurdering(periode.path("dataForVilkårsvurdering"))
            }
        }
    }

    private fun migrerVilkårsvurdering(grunnlagsdata: JsonNode) {
        if (!grunnlagsdata.isObject || grunnlagsdata.hasNonNull(medlemskapstatusKey)) return
        (grunnlagsdata as ObjectNode).put(medlemskapstatusKey, vetIkke)
    }
}
