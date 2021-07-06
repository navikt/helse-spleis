package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.serde.serdeObjectMapper
import java.time.LocalDate.EPOCH
import java.time.LocalDateTime
import java.util.*

internal class V105VilkårsgrunnlagMedGenerasjoner(
    private val uuid: UUID = UUID.randomUUID(),
    private val opprettet: LocalDateTime = EPOCH.atStartOfDay()
) : JsonMigration(version = 105) {
    override val description: String = "VilkårsgrunnlagHistorikk skal være generasjonsbasert"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        if (jsonNode["vilkårsgrunnlagHistorikk"] == null) return
        jsonNode.replace("vilkårsgrunnlagHistorikk", konverterTilInnslag(jsonNode.withArray("vilkårsgrunnlagHistorikk")))
    }

    private fun konverterTilInnslag(vilkårgrunnlag: ArrayNode): JsonNode {
        val innslagliste = serdeObjectMapper.createArrayNode()
        val innslag = serdeObjectMapper.createObjectNode()
        innslag.put("id", uuid.toString())
        innslag.put("opprettet", opprettet.toString())
        innslag.putArray("vilkårsgrunnlag").addAll(vilkårgrunnlag)
        return innslagliste.add(innslag)
    }
}
