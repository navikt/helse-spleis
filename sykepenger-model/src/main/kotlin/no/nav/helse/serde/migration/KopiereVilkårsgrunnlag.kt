package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.serde.serdeObjectMapper

internal abstract class KopiereVilkårsgrunnlag(
    versjon: Int,
    vararg vilkårsgrunnlagSomSkalKopieres: Pair<UUID, LocalDate>
): JsonMigration(version = versjon) {
    private val vilkårsgrunnlagSomSkalKopieres = vilkårsgrunnlagSomSkalKopieres.toList()

    override val description = "Kopiere inn vilkårgrunnlag for de som mangler etter V178"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val vilkårsgrunnlagHistorikk = jsonNode.path("vilkårsgrunnlagHistorikk") as ArrayNode
        val vilkårsgrunnlag = vilkårsgrunnlagHistorikk
            .flatMap { it.vilkårsgrunnlag }
            .groupBy { it.vilkårsgrunnlagId }
            .mapValues { (_, vilkårsgrunnlag) -> vilkårsgrunnlag.first() }

        val aktiveVilkårsgrunnlag = vilkårsgrunnlagHistorikk
            .firstOrNull()
            ?.vilkårsgrunnlag
            ?: serdeObjectMapper.createArrayNode()

        val aktiveSkjæringstidspunkt = aktiveVilkårsgrunnlag.map { it.skjæringstidspunkt }

        val skalKopieresPåPerson = vilkårsgrunnlagSomSkalKopieres
            .filter { (vilkårsgrunnlagId, _) -> vilkårsgrunnlagId in vilkårsgrunnlag.keys }
            .filterNot { (_, skjæringstidspunkt) -> skjæringstidspunkt in aktiveSkjæringstidspunkt }

        if (skalKopieresPåPerson.isEmpty()) return

        val oppdaterteVilkårsgrunnlag = aktiveVilkårsgrunnlag.deepCopy()

        skalKopieresPåPerson.forEach { (vilkårsgrunnlagId, skjæringstidspunkt) ->
            val vilkårsgrunnlagKopi = vilkårsgrunnlag.getValue(vilkårsgrunnlagId).deepCopy<ObjectNode>().apply {
                put("skjæringstidspunkt", "$skjæringstidspunkt")
                put("vilkårsgrunnlagId", "${UUID.randomUUID()}")
            }
            oppdaterteVilkårsgrunnlag.add(vilkårsgrunnlagKopi)
        }

        vilkårsgrunnlagHistorikk.insert(0, serdeObjectMapper.createObjectNode().apply {
            put("id", "${UUID.randomUUID()}")
            put("opprettet", "${LocalDateTime.now()}")
            putArray("vilkårsgrunnlag").addAll(oppdaterteVilkårsgrunnlag)
        })
    }

    private companion object {
        private val JsonNode.dato get() = LocalDate.parse(asText())
        private val JsonNode.vilkårsgrunnlag get() = path("vilkårsgrunnlag") as ArrayNode
        private val JsonNode.skjæringstidspunkt get() = path("skjæringstidspunkt").dato
        private val JsonNode.vilkårsgrunnlagId get() = UUID.fromString(path("vilkårsgrunnlagId").asText())
    }
}