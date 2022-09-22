package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.serde.migration.Sykefraværstilfeller.sykefraværstilfeller
import no.nav.helse.serde.migration.Sykefraværstilfeller.vedtaksperioder
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
    private val JsonNode.fraSpleis get() = path("type").asText() == "Vilkårsprøving"

    private fun List<Periode>.rettFørEllerOverlapperMed(periode: Periode) =
        any { it.overlapperMed(periode) || it.erRettFør(periode) }

    private fun List<JsonNode>.finnInfotrygdVilkårsgrunnlag(sykefraværstilfelle: Periode): JsonNode? {
        val infotrygdVilkårsgrunnlag = filter { it.fraInfotrygd }.filter { it.skjæringstidspunkt in sykefraværstilfelle }
        return infotrygdVilkårsgrunnlag.lastOrNull { it.gyldigSykepengegrunnlag } ?: infotrygdVilkårsgrunnlag.lastOrNull()
    }

    private fun List<JsonNode>.finnSpleisVilkårsgrunnlag(sykefraværstilfelle: Periode): JsonNode? {
        val skjæringstidspunkt = sykefraværstilfelle.start
        return filter { it.fraSpleis }.singleOrNull { it.skjæringstidspunkt == skjæringstidspunkt }
    }

    private fun List<JsonNode>.finnRiktigVilkårsgrunnlag(sykefraværstilfelle: Periode, perioderUtbetaltIInfotrygd: List<Periode>): JsonNode? {
        val fraInfotrygd = finnInfotrygdVilkårsgrunnlag(sykefraværstilfelle)
        val fraSpleis = finnSpleisVilkårsgrunnlag(sykefraværstilfelle)
        return when {
            fraInfotrygd == null && fraSpleis == null -> null
            fraInfotrygd != null && fraSpleis != null -> {
                if (!fraInfotrygd.gyldigSykepengegrunnlag) fraSpleis
                else if (perioderUtbetaltIInfotrygd.rettFørEllerOverlapperMed(sykefraværstilfelle)) fraInfotrygd
                else fraSpleis
            }
            else -> fraInfotrygd ?: fraSpleis
        }
    }

    internal fun brukteVilkårsgrunnlag(jsonNode: ObjectNode) : ArrayNode? {
        val vilkårsgrunnlagHistorikk = jsonNode.path("vilkårsgrunnlagHistorikk") as ArrayNode
        val sisteInnslag = vilkårsgrunnlagHistorikk.firstOrNull() ?: return null
        val aktørId = jsonNode.path("aktørId").asText()

        val vedtaksperioder = vedtaksperioder(jsonNode)
        val sykefraværstilfeller = sykefraværstilfeller(vedtaksperioder)
        val tilstanderPerSkjæringstidspunkt = vedtaksperioder.groupBy { it.skjæringstidspunkt }.mapValues { (_, vedtaksperioder) -> vedtaksperioder.map { it.tilstand() } }

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
                val vilkårgrunnlag = sorterteVilkårsgrunnlag.finnRiktigVilkårsgrunnlag(sykefraværstilfelle.periode, perioderUtbetaltIInfotrygd)
                if (vilkårgrunnlag == null) {
                    sikkerlogg.info("Fant ikke vilkårsgrunnlag for sykefraværstilfellet ${sykefraværstilfelle.periode} med tilstander ${tilstanderPerSkjæringstidspunkt[sykefraværstilfelle.tidligsteSkjæringstidspunkt]} for aktørId=$aktørId")
                    return@forEach
                }
                sykefraværstilfelle.skjæringstidspunkter.forEachIndexed {index, skjæringstidspunkt ->
                    if (vilkårgrunnlag.skjæringstidspunkt != skjæringstidspunkt) {
                        endret = true
                        sikkerlogg.info("Kopierer vilkårsgrunnlag ${vilkårgrunnlag.vilkårsgrunnlagId} fra ${vilkårgrunnlag.skjæringstidspunkt} til $skjæringstidspunkt for aktørId=$aktørId")
                    }
                    val vilkårsgrunnlagId = if (index == 0) vilkårgrunnlag.vilkårsgrunnlagId else "${UUID.randomUUID()}"

                    val vilkårsgrunnlagMedRiktigSkjæringstidspunkt = vilkårgrunnlag.deepCopy<ObjectNode>()
                        .put("skjæringstidspunkt", skjæringstidspunkt.toString())
                        .put("vilkårsgrunnlagId", vilkårsgrunnlagId)
                    add(vilkårsgrunnlagMedRiktigSkjæringstidspunkt)
                }
            }
        }

        val vilkårsgrunnlagFør = sorterteVilkårsgrunnlag.associate { it.skjæringstidspunkt to it.vilkårsgrunnlagId }
        val vilkårsgrunnlagEtter = brukteVilkårsgrunnlag.associate { it.skjæringstidspunkt to it.vilkårsgrunnlagId }
        val forkastedeVilkårsgrunnlag = vilkårsgrunnlagFør.filterNot { (_, vilkårsgrunnlagId) -> vilkårsgrunnlagId in vilkårsgrunnlagEtter.values }

        if (forkastedeVilkårsgrunnlag.isNotEmpty()){
            endret = true
            sikkerlogg.info("Forkaster vilkårsgrunnlag for skjæringstidspunkter ${forkastedeVilkårsgrunnlag.keys} med vilkårsgrunnlagIder ${forkastedeVilkårsgrunnlag.values} for aktørId=$aktørId")
        }

        return if (endret) {
            sikkerlogg.info("Endrer vilkårsgrunnlag for aktørId=$aktørId")
            brukteVilkårsgrunnlag
        } else null
    }
}



