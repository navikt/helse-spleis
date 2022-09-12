package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import no.nav.helse.hendelser.til
import no.nav.helse.serde.serdeObjectMapper
import org.slf4j.LoggerFactory

internal class V176LoggingAvForkastOgFlyttVilkårsgrunnlag : JsonMigration(version = 176) {

    override val description =
        "Logger migreringslogikk for å: " +
            "1. forkaste vilkårsgrunnlag som ikke er brukt av en vedtaksperiode" +
            "2. flytte vilkårsgrunnlag til skjæringstidspunkt, f.eks. fordi det var lagret på første utbetalingsdag i infotrygd"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val vilkårsgrunnlagHistorikk = jsonNode.path("vilkårsgrunnlagHistorikk") as ArrayNode
        val sisteInnslag = vilkårsgrunnlagHistorikk.firstOrNull() ?: return
        val aktørId = jsonNode.path("aktørId").asText()
        
        val sykefraværstilfeller = jsonNode.path("arbeidsgivere")
            .flatMap { it.path("vedtaksperioder") }
            .filterNot { it.path("tilstand").asText() == "AVSLUTTET_UTEN_UTBETALING" }
            .groupBy { it.skjæringstidspunkt }
            .map { (skjæringstidspunkt, vedtaksperioder) -> skjæringstidspunkt til vedtaksperioder.maxOf { it.tom }}

        val sorterteVilkårsgrunnlag = sisteInnslag.deepCopy<JsonNode>()
            .path("vilkårsgrunnlag")
            .sortedBy { it.skjæringstidspunkt }

        val brukteVilkårsgrunnlag = serdeObjectMapper.createArrayNode().apply {
            sykefraværstilfeller.forEach { sykefraværstilfelle ->
                val vilkårgrunnlag = sorterteVilkårsgrunnlag.lastOrNull { vilkårsgrunnlag -> vilkårsgrunnlag.skjæringstidspunkt in sykefraværstilfelle } ?: return@forEach
                val skjæringstidspunkt = sykefraværstilfelle.start
                if (vilkårgrunnlag.skjæringstidspunkt != skjæringstidspunkt) {
                    sikkerlogg.info("Flytter skjæringstidspunktet til vilkårsgrunnlag ${vilkårgrunnlag.vilkårsgrunnlagId} fra ${vilkårgrunnlag.skjæringstidspunkt} til $skjæringstidspunkt for aktørId=$aktørId")
                }
                val vilkårsgrunnlagMedRiktigSkjæringstidspunkt = (vilkårgrunnlag as ObjectNode).put("skjæringstidspunkt", skjæringstidspunkt.toString())
                add(vilkårsgrunnlagMedRiktigSkjæringstidspunkt)
            }
        }

        val forkastedeVilkårsgrunnlag = sorterteVilkårsgrunnlag.map { it.vilkårsgrunnlagId } - brukteVilkårsgrunnlag.map { it.vilkårsgrunnlagId }
        sikkerlogg.info("Forkaster vilkårsgrunnlag $forkastedeVilkårsgrunnlag for aktørId=$aktørId")

    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

        private val JsonNode.dato get() = LocalDate.parse(asText())
        private val JsonNode.skjæringstidspunkt get() = path("skjæringstidspunkt").dato
        private val JsonNode.tom get() = path("tom").dato
        private val JsonNode.vilkårsgrunnlagId get() = path("vilkårsgrunnlagId").asText()
    }
}