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
    private val JsonNode.fom get() = path("fom").dato
    private val JsonNode.tom get() = path("tom").dato
    private val JsonNode.vilkårsgrunnlagId get() = path("vilkårsgrunnlagId").asText()
    private val JsonNode.fraInfotrygd get() = path("type").asText() == "Infotrygd"
    private val JsonNode.gyldigSykepengegrunnlag get() = path("sykepengegrunnlag").path("sykepengegrunnlag").asDouble() > 0
    private val JsonNode.gyldigAvviksprosent get() = path("avviksprosent").asDouble() != Double.POSITIVE_INFINITY
    private val JsonNode.fraSpleis get() = path("type").asText() == "Vilkårsprøving"
    private val JsonNode.tilstand get() = path("tilstand").asText()


    private fun List<JsonNode>.finnInfotrygdVilkårsgrunnlag(sykefraværstilfelle: Periode, perioderUtbetaltIInfotrygd: List<Periode>): JsonNode? {
        if (perioderUtbetaltIInfotrygd.none { it.overlapperMed(sykefraværstilfelle) || it.erRettFør(sykefraværstilfelle) }) return null
        return filter { it.fraInfotrygd && it.gyldigSykepengegrunnlag }.lastOrNull { vilkårsgrunnlag -> vilkårsgrunnlag.skjæringstidspunkt in sykefraværstilfelle }
    }

    private fun List<JsonNode>.finnSpleisVilkårsgrunnlag(sykefraværstilfelle: Periode): JsonNode? {
        val skjæringstidspunkt = sykefraværstilfelle.start
        return filter { it.fraSpleis && it.gyldigAvviksprosent }.singleOrNull { it.skjæringstidspunkt == skjæringstidspunkt }
    }

    private fun List<JsonNode>.finnRiktigVilkårsgrunnlag(sykefraværstilfelle: Periode, perioderUtbetaltIInfotrygd: List<Periode>): JsonNode? {
        val fraInfotrygd = finnInfotrygdVilkårsgrunnlag(sykefraværstilfelle, perioderUtbetaltIInfotrygd)
        return fraInfotrygd ?: finnSpleisVilkårsgrunnlag(sykefraværstilfelle)
    }


    internal fun brukteVilkårsgrunnlag(jsonNode: ObjectNode) : ArrayNode? {
        val vilkårsgrunnlagHistorikk = jsonNode.path("vilkårsgrunnlagHistorikk") as ArrayNode
        val sisteInnslag = vilkårsgrunnlagHistorikk.firstOrNull() ?: return null
        val aktørId = jsonNode.path("aktørId").asText()

        val vedtaksperioderPerSkjæringstidspunkt = jsonNode.path("arbeidsgivere")
            .flatMap { it.path("vedtaksperioder") }
            .filterNot { it.tilstand == "AVSLUTTET_UTEN_UTBETALING" }
            .groupBy { it.skjæringstidspunkt }

        val sykefraværstilfeller = vedtaksperioderPerSkjæringstidspunkt
            .map { (skjæringstidspunkt, vedtaksperioder) ->
                skjæringstidspunkt til vedtaksperioder.maxOf { it.tom }
            }

        val tilstanderPerSkjæringstidspunkt = vedtaksperioderPerSkjæringstidspunkt
            .mapValues { (_, vedtaksperioder) ->
                val førsteFom = vedtaksperioder.minOf { it.fom }
                vedtaksperioder.filter { it.fom == førsteFom }.map { it.tilstand }
            }

        val perioderUtbetaltIInfotrygd = jsonNode.path("infotrygdhistorikk")
            .firstOrNull()?.let { sisteInfotrygdInnslag ->
                val arbeidsgiverutbetalingsperioder = sisteInfotrygdInnslag.path("arbeidsgiverutbetalingsperioder").map { it.fom til it.tom }
                val personutbetalingsperioder = sisteInfotrygdInnslag.path("personutbetalingsperioder").map { it.fom til it.tom }
                arbeidsgiverutbetalingsperioder + personutbetalingsperioder
            } ?: emptyList()


        val sorterteVilkårsgrunnlag = sisteInnslag.deepCopy<JsonNode>()
            .path("vilkårsgrunnlag")
            .sortedBy { it.skjæringstidspunkt }

        var endret = false

        val brukteVilkårsgrunnlag = serdeObjectMapper.createArrayNode().apply {
            sykefraværstilfeller.forEach { sykefraværstilfelle ->
                val vilkårgrunnlag = sorterteVilkårsgrunnlag.finnRiktigVilkårsgrunnlag(sykefraværstilfelle, perioderUtbetaltIInfotrygd)
                if (vilkårgrunnlag == null) {
                    sikkerlogg.info("Fant ikke vilkårsgrunnlag for sykefraværstilfellet $sykefraværstilfelle med tilstander ${tilstanderPerSkjæringstidspunkt[sykefraværstilfelle.start]} for aktørId=$aktørId")
                    return@forEach
                }
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



