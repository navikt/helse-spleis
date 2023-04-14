package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.util.UUID
import org.slf4j.LoggerFactory

internal class V237KopiereRefusjonsopplysningerTilEldreInnslag: JsonMigration(237) {
    override val description = "V196 innførte refusjonsopplysninger kun på nyeste innslaget den gangen. Denne migreringen kopierer de inn på eldre innslag også"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val vilkårsgrunnlagMedSykepengegrunnlag = mutableMapOf<UUID, JsonNode>()
        jsonNode.path("vilkårsgrunnlagHistorikk").forEach { innslag ->
            innslag.path("vilkårsgrunnlag").forEach { elementet ->
                val uuid = UUID.fromString(elementet.path("vilkårsgrunnlagId").asText())
                if (uuid !in vilkårsgrunnlagMedSykepengegrunnlag) {
                    vilkårsgrunnlagMedSykepengegrunnlag.getOrPut(uuid) {
                        elementet.path("sykepengegrunnlag")
                    }
                } else {
                    // erstatter sykepengegrunnlaget på eldre innslag med det nyeste for vilkårsgrunnlagId
                    (elementet as ObjectNode).replace("sykepengegrunnlag", vilkårsgrunnlagMedSykepengegrunnlag.getValue(uuid).deepCopy())
                }
            }
        }
    }

}