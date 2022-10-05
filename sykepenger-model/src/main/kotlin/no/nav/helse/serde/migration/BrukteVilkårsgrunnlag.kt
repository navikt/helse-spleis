package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.erRettFør
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.serde.migration.Sykefraværstilfeller.Sykefraværstilfelle
import no.nav.helse.serde.migration.Sykefraværstilfeller.Vedtaksperiode.Companion.aktiveSkjæringstidspunkter
import no.nav.helse.serde.migration.Sykefraværstilfeller.sykefraværstilfeller
import no.nav.helse.serde.migration.Sykefraværstilfeller.vedtaksperioder
import no.nav.helse.serde.serdeObjectMapper
import org.slf4j.LoggerFactory

internal object BrukteVilkårsgrunnlag {

    private val JsonNode.dato get() = LocalDate.parse(asText())
    private val JsonNode.skjæringstidspunkt get() = path("skjæringstidspunkt").dato
    private val JsonNode.fom get() = path("fom").dato
    private val JsonNode.tom get() = path("tom").dato
    private val JsonNode.vilkårsgrunnlagId get() = path("vilkårsgrunnlagId").asText()
    private val JsonNode.fraInfotrygd get() = path("type").asText() == "Infotrygd"
    private val JsonNode.gyldigSykepengegrunnlag get() = path("sykepengegrunnlag").path("sykepengegrunnlag").asDouble() > 0
    private val JsonNode.fraSpleis get() = path("type").asText() == "Vilkårsprøving"
    private val JsonNode.vilkårsgrunnlagstype get() = if (fraSpleis) "Spleisvilkårsgrunnlag" else "Infotrygdvilkårsgrunnlag"

    private fun List<Periode>.rettFørEllerOverlapperMed(periode: Periode) =
        any { it.overlapperMed(periode) || it.erRettFør(periode) }

    private fun List<JsonNode>.finnInfotrygdVilkårsgrunnlag(sykefraværstilfelle: Periode): JsonNode? {
        val infotrygdVilkårsgrunnlag = this
            .filter { it.fraInfotrygd }
            .filter { it.skjæringstidspunkt in sykefraværstilfelle || sykefraværstilfelle.endInclusive.erRettFør(it.skjæringstidspunkt) }
        return infotrygdVilkårsgrunnlag.lastOrNull { it.gyldigSykepengegrunnlag } ?: infotrygdVilkårsgrunnlag.lastOrNull()
    }

    private fun List<JsonNode>.finnSpleisVilkårsgrunnlag(skjæringtidspunkter: Set<LocalDate>) =
        filter { it.fraSpleis }.lastOrNull { it.skjæringstidspunkt in skjæringtidspunkter }

    private fun List<JsonNode>.finnSpleisVilkårsgrunnlag(sykefraværstilfelle: Periode) =
        filter { it.fraSpleis }.lastOrNull { it.skjæringstidspunkt in sykefraværstilfelle }

    private fun List<JsonNode>.finnRiktigVilkårsgrunnlag(
        sykefraværstilfelle: Sykefraværstilfelle,
        perioderUtbetaltIInfotrygd: List<Periode>,
        periode: (sykefraværstilfelle: Sykefraværstilfelle) -> Periode,
        ingenVilkårsgrunnlag: (sorterteVilkårgrunnlag: List<JsonNode>) -> JsonNode? = { null }): JsonNode? {
        val benyttetPeriode = periode(sykefraværstilfelle)
        val fraInfotrygd = finnInfotrygdVilkårsgrunnlag(benyttetPeriode)
        val fraSpleis = finnSpleisVilkårsgrunnlag(sykefraværstilfelle.skjæringstidspunkter)
        return when {
            fraInfotrygd == null && fraSpleis == null -> ingenVilkårsgrunnlag(this)
            fraInfotrygd != null && fraSpleis != null -> {
                if (!fraInfotrygd.gyldigSykepengegrunnlag) fraSpleis
                else if (perioderUtbetaltIInfotrygd.rettFørEllerOverlapperMed(benyttetPeriode)) fraInfotrygd
                else fraSpleis
            }
            else -> fraInfotrygd ?: fraSpleis
        }
    }

    internal fun brukteVilkårsgrunnlag(jsonNode: ObjectNode, id: String) : ArrayNode? {
        val sikkerlogg = Sikkerlogg(aktørId = jsonNode.path("aktørId").asText(), id = id)
        val vilkårsgrunnlagHistorikk = jsonNode.path("vilkårsgrunnlagHistorikk") as ArrayNode
        val sisteInnslag = vilkårsgrunnlagHistorikk.firstOrNull() ?: return sikkerlogg.info("Har ingen innslag i vilkårsgrunnlaghistorikken").let { null }

        val vedtaksperioder = vedtaksperioder(jsonNode)
        val aktiveSkjæringstidspunkter = vedtaksperioder.aktiveSkjæringstidspunkter()
        val sykefraværstilfeller = sykefraværstilfeller(vedtaksperioder)
        sikkerlogg.info("Fant ${sykefraværstilfeller.size} sykefraværstilfeller ${sykefraværstilfeller.map { it.periode }}")
        val tilstanderPerSkjæringstidspunkt = vedtaksperioder
            .groupBy { it.skjæringstidspunkt }
            .mapValues { (_, vedtaksperioder) ->
                val førsteFom = vedtaksperioder.minOf { it.periode.start }
                vedtaksperioder.filter { it.periode.start == førsteFom }
            }
            .mapValues { (_, vedtaksperioder) -> vedtaksperioder.map { it.tilstand() } }

        val perioderUtbetaltIInfotrygd = jsonNode.path("infotrygdhistorikk")
            .firstOrNull()?.let { sisteInfotrygdInnslag ->
                val arbeidsgiverutbetalingsperioder = sisteInfotrygdInnslag.path("arbeidsgiverutbetalingsperioder").map { it.fom til it.tom }
                val personutbetalingsperioder = sisteInfotrygdInnslag.path("personutbetalingsperioder").map { it.fom til it.tom }
                arbeidsgiverutbetalingsperioder + personutbetalingsperioder
            } ?: emptyList()


        val sorterteVilkårsgrunnlag = sisteInnslag.deepCopy<JsonNode>()
            .path("vilkårsgrunnlag")
            .sortedBy { it.skjæringstidspunkt }

        val skjæringstidspunktLagtTil = mutableSetOf<LocalDate>()

        var endret = false

        val brukteVilkårsgrunnlag = serdeObjectMapper.createArrayNode().apply {
            sykefraværstilfeller.forEach { sykefraværstilfelle ->
                val vilkårgrunnlag =
                    sorterteVilkårsgrunnlag.finnRiktigVilkårsgrunnlag(sykefraværstilfelle, perioderUtbetaltIInfotrygd, { it.periodeFremTilSisteDagISpleis })
                    ?:sorterteVilkårsgrunnlag.finnRiktigVilkårsgrunnlag(sykefraværstilfelle, perioderUtbetaltIInfotrygd, { it.periode })
                    ?:sorterteVilkårsgrunnlag.finnRiktigVilkårsgrunnlag(sykefraværstilfelle, perioderUtbetaltIInfotrygd, { it.periodeFremTilSisteDagISpleis }) { it.finnSpleisVilkårsgrunnlag(sykefraværstilfelle.periode)}

                if (vilkårgrunnlag == null) {
                    sikkerlogg.info("Fant ikke vilkårsgrunnlag for sykefraværstilfellet ${sykefraværstilfelle.periode} med tilstander ${tilstanderPerSkjæringstidspunkt[sykefraværstilfelle.tidligsteSkjæringstidspunkt]}")
                    return@forEach
                }
                sykefraværstilfelle.skjæringstidspunkter.filter { it in aktiveSkjæringstidspunkter }.forEachIndexed { index, skjæringstidspunkt ->
                    if (skjæringstidspunkt in skjæringstidspunktLagtTil) return@forEachIndexed

                    if (vilkårgrunnlag.skjæringstidspunkt != skjæringstidspunkt) {
                        endret = true
                        sikkerlogg.info("Kopierer ${vilkårgrunnlag.vilkårsgrunnlagstype} ${vilkårgrunnlag.vilkårsgrunnlagId} fra ${vilkårgrunnlag.skjæringstidspunkt} til $skjæringstidspunkt ifbm. sykefraværstilfellet ${sykefraværstilfelle.periode}")
                    }
                    val vilkårsgrunnlagId = if (index == 0) vilkårgrunnlag.vilkårsgrunnlagId else {
                        endret = true
                        "${UUID.randomUUID()}"
                    }
                    val vilkårsgrunnlagMedRiktigSkjæringstidspunkt = vilkårgrunnlag.deepCopy<ObjectNode>()
                        .put("skjæringstidspunkt", "$skjæringstidspunkt")
                        .put("vilkårsgrunnlagId", vilkårsgrunnlagId)

                    skjæringstidspunktLagtTil.add(skjæringstidspunkt)
                    add(vilkårsgrunnlagMedRiktigSkjæringstidspunkt)
                }
            }
        }

        val vilkårsgrunnlagFør = sorterteVilkårsgrunnlag.associate { it.skjæringstidspunkt to it.vilkårsgrunnlagId }
        val vilkårsgrunnlagEtter = brukteVilkårsgrunnlag.associate { it.skjæringstidspunkt to it.vilkårsgrunnlagId }
        val forkastedeVilkårsgrunnlag = vilkårsgrunnlagFør.filterNot { (_, vilkårsgrunnlagId) -> vilkårsgrunnlagId in vilkårsgrunnlagEtter.values }

        if (forkastedeVilkårsgrunnlag.isNotEmpty()){
            endret = true
            sikkerlogg.info("Forkaster vilkårsgrunnlag for skjæringstidspunkter ${forkastedeVilkårsgrunnlag.keys} med vilkårsgrunnlagIder ${forkastedeVilkårsgrunnlag.values}")
        }

        return if (endret) {
            sikkerlogg.info("Legger til nytt innslag i vilkårsgrunnlaghistorikken for skjæringstidspunkter ${vilkårsgrunnlagEtter.keys}")
            brukteVilkårsgrunnlag
        } else {
            sikkerlogg.info("Trenger ikke nytt innslag i vilkårsgrunnlaghistorikken")
            null
        }
    }

    private class Sikkerlogg(private val aktørId: String, private val id: String) {
        fun info(melding: String) = sikkerlogg.info("$melding for {}", keyValue("aktørId", aktørId), keyValue("module", id))

        private companion object {
            private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
        }
    }
}
