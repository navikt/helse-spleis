package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.til
import no.nav.helse.serde.serdeObjectMapper

internal class V176ForkastUbrukteVilkårsgrunnlag : JsonMigration(version = 176) {

    override val description =
        "Forkaste vilkårsgrunnlag som ikke er brukt i en aktiv vedtaksperiode"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val vilkårsgrunnlagHistorikk = jsonNode.path("vilkårsgrunnlagHistorikk") as ArrayNode
        val sisteInnslag = vilkårsgrunnlagHistorikk.firstOrNull() ?: return

        val aktiveVedtaksperioder = jsonNode.path("arbeidsgivere")
            .flatMap { it.path("vedtaksperioder") }
            .filterNot { it.path("tilstand").asText() == "AVSLUTTET_UTEN_UTBETALING" }

        val sorterteVilkårsgrunnlag = sisteInnslag.deepCopy<JsonNode>()
            .path("vilkårsgrunnlag")
            .sortedBy { it.skjæringstidspunkt }

        val brukteVilkårsgrunnlag = sorterteVilkårsgrunnlag.filterIndexed { index, vilkårsgrunnlag ->
            val vilkårsgrunnlagSkjæringstidspunkt = vilkårsgrunnlag.skjæringstidspunkt
            val nesteVilkårsgrunnlagSkjæringstidspunkt = sorterteVilkårsgrunnlag.getOrNull(index + 1)?.skjæringstidspunkt
            aktiveVedtaksperioder.any { it.brukerVilkårsgrunnlag(vilkårsgrunnlagSkjæringstidspunkt, nesteVilkårsgrunnlagSkjæringstidspunkt) }
        }

        val nyttInnslag = serdeObjectMapper.createObjectNode().apply {
            put("id", "${UUID.randomUUID()}")
            put("opprettet", "${LocalDateTime.now()}")
            putArray("vilkårsgrunnlag").addAll(brukteVilkårsgrunnlag)
        }

        vilkårsgrunnlagHistorikk.insert(0, nyttInnslag)
    }

    private companion object {
        private val JsonNode.dato get() = LocalDate.parse(asText())
        private val JsonNode.skjæringstidspunkt get() = path("skjæringstidspunkt").dato
        private val JsonNode.tom get() = path("tom").dato

        private fun JsonNode.brukerVilkårsgrunnlag(
            vilkårsgrunnlagSkjæringstidspunkt: LocalDate,
            nesteVilkårsgrunnlagSkjæringstidspunkt: LocalDate?) : Boolean {
            val periode = skjæringstidspunkt til tom
            if (nesteVilkårsgrunnlagSkjæringstidspunkt in periode) return false
            return vilkårsgrunnlagSkjæringstidspunkt in periode
        }
    }
}