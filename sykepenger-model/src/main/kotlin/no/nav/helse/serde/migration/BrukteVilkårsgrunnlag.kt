package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.serde.serdeObjectMapper
import org.slf4j.LoggerFactory

internal object BrukteVilkårsgrunnlag {

    private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

    private val JsonNode.dato get() = LocalDate.parse(asText())
    private val JsonNode.skjæringstidspunkt get() = path("skjæringstidspunkt").dato
    private val JsonNode.tom get() = path("tom").dato
    private val JsonNode.vilkårsgrunnlagId get() = path("vilkårsgrunnlagId").asText()
    private val JsonNode.fraInfotrygd get() = path("type").asText() == "Infotrygd"
    private val JsonNode.fraSpleis get() = path("type").asText() == "Vilkårsprøving"


    private fun List<JsonNode>.loggSprøInfotrygdVilkårsgrunnlag(sykefraværstilfelle: Periode, aktørId: String) {
        val vilkårsgrunnlagFraInfotrygd = filter { it.fraInfotrygd && it.skjæringstidspunkt in sykefraværstilfelle}
        if (vilkårsgrunnlagFraInfotrygd.size > 1) {
            sikkerlogg.info("Fant ${vilkårsgrunnlagFraInfotrygd.size} vilkårsgrunnlag fra infotrygd innenfor sykefraværstilfelle $sykefraværstilfelle for aktørId=$aktørId")
        }
    }

    private fun List<JsonNode>.loggSprøSpleisVilkårsgrunnlag(sykefraværstilfelle: Periode, aktørId: String) {
        val skjæringstidspunkt = sykefraværstilfelle.start
        val vilkårsgrunnlagFraSpleis = filter { it.fraSpleis && it.skjæringstidspunkt == skjæringstidspunkt }
        if (vilkårsgrunnlagFraSpleis.size > 1) {
            sikkerlogg.info("Fant ${vilkårsgrunnlagFraSpleis.size} vilkårsgrunnlag fra spleis på skjæringstidspunkt $skjæringstidspunkt for aktørId=$aktørId")
        }
    }


    private fun List<JsonNode>.finnRiktigVilkårsgrunnlag(sykefraværstilfelle: Periode): JsonNode? {
        val skjæringstidspunkt = sykefraværstilfelle.start

        val fraInfotrygd = filter { it.fraInfotrygd }.lastOrNull { vilkårsgrunnlag -> vilkårsgrunnlag.skjæringstidspunkt in sykefraværstilfelle }

        return fraInfotrygd ?: filter { it.fraSpleis }.singleOrNull { it.skjæringstidspunkt == skjæringstidspunkt }
    }


    internal fun brukteVilkårsgrunnlag(jsonNode: ObjectNode) : ArrayNode? {
        val vilkårsgrunnlagHistorikk = jsonNode.path("vilkårsgrunnlagHistorikk") as ArrayNode
        val sisteInnslag = vilkårsgrunnlagHistorikk.firstOrNull() ?: return null
        val aktørId = jsonNode.path("aktørId").asText()

        val sykefraværstilfeller = jsonNode.path("arbeidsgivere")
            .flatMap { it.path("vedtaksperioder") }
            .filterNot { it.path("tilstand").asText() == "AVSLUTTET_UTEN_UTBETALING" }
            .groupBy { it.skjæringstidspunkt }
            .map { (skjæringstidspunkt, vedtaksperioder) -> skjæringstidspunkt til vedtaksperioder.maxOf { it.tom }}

        val sorterteVilkårsgrunnlag = sisteInnslag.deepCopy<JsonNode>()
            .path("vilkårsgrunnlag")
            .sortedBy { it.skjæringstidspunkt }

        var endret = false

        val brukteVilkårsgrunnlag = serdeObjectMapper.createArrayNode().apply {
            sykefraværstilfeller.forEach { sykefraværstilfelle ->
                sorterteVilkårsgrunnlag.loggSprøInfotrygdVilkårsgrunnlag(sykefraværstilfelle, aktørId)
                sorterteVilkårsgrunnlag.loggSprøSpleisVilkårsgrunnlag(sykefraværstilfelle, aktørId)
                val vilkårgrunnlag = sorterteVilkårsgrunnlag.finnRiktigVilkårsgrunnlag(sykefraværstilfelle) ?: return@forEach
                val skjæringstidspunkt = sykefraværstilfelle.start
                if (vilkårgrunnlag.skjæringstidspunkt != skjæringstidspunkt) {
                    endret = true
                    sikkerlogg.info("Flytter skjæringstidspunktet til vilkårsgrunnlag ${vilkårgrunnlag.vilkårsgrunnlagId} fra ${vilkårgrunnlag.skjæringstidspunkt} til $skjæringstidspunkt for aktørId=$aktørId")
                }
                val vilkårsgrunnlagMedRiktigSkjæringstidspunkt = (vilkårgrunnlag as ObjectNode).put("skjæringstidspunkt", skjæringstidspunkt.toString())
                add(vilkårsgrunnlagMedRiktigSkjæringstidspunkt)
            }
        }

        val forkastedeVilkårsgrunnlag = sorterteVilkårsgrunnlag.map { it.vilkårsgrunnlagId } - brukteVilkårsgrunnlag.map { it.vilkårsgrunnlagId }
        if (forkastedeVilkårsgrunnlag.isNotEmpty()){
            endret = true
            sikkerlogg.info("Forkaster vilkårsgrunnlag $forkastedeVilkårsgrunnlag for aktørId=$aktørId")
        }

        return if (endret) {
            sikkerlogg.info("Endrer vilkårsgrunnlag for aktørId=$aktørId")
            brukteVilkårsgrunnlag
        } else null
    }
}



