package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.serde.migration.RefusjonsopplysningerIVilkårsgrunnlag.vilkårsgrunnlagMedRefusjonsopplysninger
import no.nav.helse.serde.serdeObjectMapper

internal class V196RefusjonsopplysningerIVilkårsgrunnlag: JsonMigration(version = 196) {

    override val description =
        "Legger til refusjonsopplysninger i eksisterende vilkårsgrunnlag basert på det som finnes i refusjonshistorikken for arbeidsgiverne i sykepengegrunnlaget"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val vilkårsgunnlagMedRefusjonsopplysninger = vilkårsgrunnlagMedRefusjonsopplysninger(jsonNode) ?: return
        val vilkårsgrunnlagHistorikk = jsonNode.path("vilkårsgrunnlagHistorikk") as ArrayNode

        val nyttInnslag = serdeObjectMapper.createObjectNode().apply {
            put("id", "${UUID.randomUUID()}")
            put("opprettet", "${LocalDateTime.now()}")
            putArray("vilkårsgrunnlag").addAll(vilkårsgunnlagMedRefusjonsopplysninger)
        }

        vilkårsgrunnlagHistorikk.insert(0, nyttInnslag)
    }
}