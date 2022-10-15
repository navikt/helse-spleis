package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import org.slf4j.LoggerFactory
import org.slf4j.MDC

private typealias InnslagId = UUID
private typealias VilkårsgrunnlagId = UUID
private typealias BeregningId = UUID
private typealias UtbetalingId = UUID
private typealias VedtaksperiodeId = UUID

internal class V185UtbetalingerOgVilkårsgrunnlag: JsonMigration(185) {
    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
        private fun withMDC(context: Map<String, String>, block: () -> Unit) {
            val contextMap = MDC.getCopyOfContextMap() ?: emptyMap()
            try {
                MDC.setContextMap(contextMap + context)
                block()
            } finally {
                MDC.setContextMap(contextMap)
            }
        }

    }
    override val description = "DRY RUN - Migrerer vedtaksperiodeutbetalinger til å ha vilkårsgrunnlagId"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val aktørId = jsonNode.path("aktørId").asText()
        withMDC(mapOf("aktørId" to aktørId)) {
            utførMigrering(aktørId, jsonNode)
        }
    }

    private fun utførMigrering(aktørId: String, jsonNode: ObjectNode) {
        val innslagRekkefølge = jsonNode
            .path("vilkårsgrunnlagHistorikk")
            .mapIndexed { index, innslag ->
                index to UUID.fromString(innslag.path("id").asText()) as InnslagId
            }
            .toMap()
        val vilkårsgrunnlag = jsonNode
            .path("vilkårsgrunnlagHistorikk")
            .associateBy({ UUID.fromString(it.path("id").asText()) as InnslagId }) { innslag ->
                innslag.path("vilkårsgrunnlag").associateBy({ UUID.fromString(it.path("vilkårsgrunnlagId").asText()) as VilkårsgrunnlagId }) { grunnlag ->
                    Vilkårsgrunnlag(
                        innslagId = UUID.fromString(innslag.path("id").asText()) as InnslagId,
                        vilkårsgrunnlagId = UUID.fromString(grunnlag.path("vilkårsgrunnlagId").asText()) as VilkårsgrunnlagId,
                        skjæringstidspunkt = LocalDate.parse(grunnlag.path("skjæringstidspunkt").asText()),
                        fraInfotrygd = grunnlag.path("type").asText() == "Infotrygd"
                    )
                }
            }

        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val beregningTilInnslagId = arbeidsgiver
                .path("beregnetUtbetalingstidslinjer")
                .associateBy({ UUID.fromString(it.path("id").asText()) as BeregningId }) {
                    UUID.fromString(it.path("vilkårsgrunnlagHistorikkInnslagId").asText()) as InnslagId
                }
            val utbetalingTilInnslag = arbeidsgiver
                .path("utbetalinger")
                .associateBy({ UUID.fromString(it.path("id").asText()) as UtbetalingId }) { utbetaling ->
                    val beregningId = UUID.fromString(utbetaling.path("beregningId").asText()) as BeregningId
                    beregningTilInnslagId[beregningId].also {
                        if (it == null) {
                            sikkerlogg.info("finner ikke vilkårsgrunnlagInnslagId for utbetaling=${utbetaling.path("id").asText()} for aktørId=$aktørId")
                        }
                    }
                }

            arbeidsgiver.path("vedtaksperioder").forEach {
                migrerVedtaksperiode(innslagRekkefølge, vilkårsgrunnlag, utbetalingTilInnslag, it)
            }
            arbeidsgiver.path("forkastede").forEach { forkastet ->
                migrerVedtaksperiode(innslagRekkefølge, vilkårsgrunnlag, utbetalingTilInnslag, forkastet.path("vedtaksperiode"))
            }
        }
    }

    private fun migrerVedtaksperiode(
        innslagRekkefølge: Map<Int, InnslagId>,
        vilkårsgrunnlag: Map<InnslagId, Map<VilkårsgrunnlagId, Vilkårsgrunnlag>>,
        utbetalingTilInnslag: Map<UtbetalingId, InnslagId?>,
        vedtaksperiode: JsonNode
    ) {
        val vedtaksperiodeId = UUID.fromString(vedtaksperiode.path("id").asText()) as VedtaksperiodeId
        val skjæringstidspunktVedtaksperiode = LocalDate.parse(vedtaksperiode.path("skjæringstidspunkt").asText())
        val førstedatoVedtaksperiode = LocalDate.parse(vedtaksperiode.path("fom").asText())
        val sistedatoVedtaksperiode = LocalDate.parse(vedtaksperiode.path("tom").asText())
        val søkeperiode = minOf(skjæringstidspunktVedtaksperiode, førstedatoVedtaksperiode) til sistedatoVedtaksperiode
        val vilkårsgrunnlagVedtaksperiode = innslagRekkefølge.keys.sorted().firstNotNullOfOrNull { index ->
            val innslagId = innslagRekkefølge.getValue(index)
            vilkårsgrunnlag.getValue(innslagId).values.firstOrNull { vilkårsgrunnlag ->
                vilkårsgrunnlag.skjæringstidspunkt == skjæringstidspunktVedtaksperiode
            }
        }

        vedtaksperiode
            .path("utbetalinger")
            .forEach { utbetaling ->
                val vilkårsgrunnlagId = utbetaling.path("vilkårsgrunnlagId").takeUnless { it.isNull || it.isMissingNode }?.asText()
                val utbetalingId = UUID.fromString(utbetaling.path("utbetalingId").asText()) as UtbetalingId
                if (vilkårsgrunnlagId == null) {
                    val innslagId = utbetalingTilInnslag.getValue(utbetalingId)
                    if (innslagId != null) {
                        val match = finnVilkårsgrunnlagForUtbetaling(vilkårsgrunnlag, innslagId, utbetalingId, vedtaksperiodeId, vilkårsgrunnlagVedtaksperiode, skjæringstidspunktVedtaksperiode, søkeperiode)

                        if (match != null) {
                            // når ikke dry-run:
                            // (utbetaling as ObjectNode).put("vilkårsgrunnlagId", match.vilkårsgrunnlagId.toString())
                        }
                    }
                }
            }
    }

    private fun finnVilkårsgrunnlagForUtbetaling(
        vilkårsgrunnlag: Map<InnslagId, Map<VilkårsgrunnlagId, Vilkårsgrunnlag>>,
        innslagId: InnslagId,
        utbetalingId: UtbetalingId,
        vedtaksperiodeId: VedtaksperiodeId,
        vilkårsgrunnlagVedtaksperiode: Vilkårsgrunnlag?,
        skjæringstidspunktVedtaksperiode: LocalDate,
        søkeperiode: Periode
    ): Vilkårsgrunnlag? {
        val ufiltrertListe = vilkårsgrunnlag[innslagId]?.values
        val filtrertListe = ufiltrertListe?.filter { grunnlag ->
            // om vedtaksperioden stammer fra Infotrygd (eller ikke stammer fra Infotrygd), så skal alle
            // utbetalingene også gjøre det
            vilkårsgrunnlagVedtaksperiode == null || grunnlag.fraInfotrygd == vilkårsgrunnlagVedtaksperiode.fraInfotrygd
        } ?: return null

        val direkteMatch = filtrertListe.firstOrNull { grunnlag -> grunnlag.skjæringstidspunkt == skjæringstidspunktVedtaksperiode }
        if (direkteMatch != null) {
            loggMatch("fant direkte match vilkårsgrunnlag=${direkteMatch.vilkårsgrunnlagId} skjæringstidspunkt=${direkteMatch.skjæringstidspunkt} for", utbetalingId, vedtaksperiodeId, skjæringstidspunktVedtaksperiode)
            return direkteMatch
        }

        val direkteMatchUfiltrert = ufiltrertListe.firstOrNull { grunnlag -> grunnlag.skjæringstidspunkt == skjæringstidspunktVedtaksperiode }
        val indirekteMatchUfiltrert = ufiltrertListe.firstOrNull { grunnlag -> grunnlag.skjæringstidspunkt in søkeperiode }

        val indirekteMatch = filtrertListe.firstOrNull { grunnlag -> grunnlag.skjæringstidspunkt in søkeperiode }
        if (indirekteMatch != null) loggMatch("fant match i søkeperiode direkteMatchUfiltrert=${direkteMatchUfiltrert != null} vilkårsgrunnlag=${indirekteMatch.vilkårsgrunnlagId} skjæringstidspunkt=${indirekteMatch.skjæringstidspunkt} for", utbetalingId, vedtaksperiodeId, skjæringstidspunktVedtaksperiode)
        else loggMatch("fant ikke vilkårsgrunnlag direkteMatchUfiltrert=${direkteMatchUfiltrert != null} indirekteMatchUfiltrert=${indirekteMatchUfiltrert != null}  for", utbetalingId, vedtaksperiodeId, skjæringstidspunktVedtaksperiode)

        return indirekteMatch
    }

    private fun loggMatch(
        tekst: String,
        utbetalingId: UtbetalingId,
        vedtaksperiodeId: VedtaksperiodeId,
        skjæringstidspunktVedtaksperiode: LocalDate
    ) {
        sikkerlogg.info("$tekst utbetaling=$utbetalingId for vedtaksperiode=$vedtaksperiodeId med vedtaksperiodeSkjæringstidspunkt=$skjæringstidspunktVedtaksperiode")
    }

    private class Vilkårsgrunnlag(
        val innslagId: InnslagId,
        val vilkårsgrunnlagId: VilkårsgrunnlagId,
        val skjæringstidspunkt: LocalDate,
        val fraInfotrygd: Boolean
    )
}