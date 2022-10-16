package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioderMedHensynTilHelg
import no.nav.helse.hendelser.til
import org.slf4j.LoggerFactory
import org.slf4j.MDC

private typealias InnslagId = UUID
private typealias VilkårsgrunnlagId = UUID
private typealias BeregningId = UUID
private typealias UtbetalingId = UUID
private typealias VedtaksperiodeId = UUID

internal class V190UtbetalingerOgVilkårsgrunnlag: JsonMigration(190) {
    private companion object {
        private val ingenInnslagId = UUID.fromString("00000000-0000-0000-0000-000000000000") as InnslagId
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
        val vedtaksperioderForPerson = Sykefraværstilfeller.vedtaksperioder(jsonNode)
        val sykefraværstilfeller = Sykefraværstilfeller.sykefraværstilfeller(vedtaksperioderForPerson)
        val innslagTidsstempel = jsonNode
            .path("vilkårsgrunnlagHistorikk")
            .map {
                val id = UUID.fromString(it.path("id").asText()) as InnslagId
                val tidsstempel = LocalDateTime.parse(it.path("opprettet").asText())
                id to tidsstempel
            }
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

        val infotrygdinntekter = jsonNode
            .path("infotrygdhistorikk")
            .map { element ->
                val opprettet = LocalDateTime.parse(element.path("tidsstempel").asText())
                Infotrygdhistorikk(
                    tidsstempel = opprettet,
                    inntekter = element.path("inntekter").map { inntekt ->
                        LocalDate.parse(inntekt.path("sykepengerFom").asText())
                    }
                )
            }

        val sammenhengendePerioder = jsonNode.path("arbeidsgivere")
            .flatMap { sammenhengendePerioder(it) }
            .grupperSammenhengendePerioderMedHensynTilHelg()

        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val beregningTilInnslagId = arbeidsgiver
                .path("beregnetUtbetalingstidslinjer")
                .associateBy({ UUID.fromString(it.path("id").asText()) as BeregningId }) { beregning ->
                    val opprettet = LocalDateTime.parse(beregning.path("tidsstempel").asText())
                    (UUID.fromString(beregning.path("vilkårsgrunnlagHistorikkInnslagId").asText()) as InnslagId)
                        .takeUnless { id -> id == ingenInnslagId }
                        ?: innslagTidsstempel.firstOrNull { (_, tidsstempel) ->
                            tidsstempel < opprettet
                        }?.first
                        ?: innslagTidsstempel.lastOrNull()?.first // se i eldste innslag som siste utvei
                }
            val utbetalingTilInnslag = arbeidsgiver
                .path("utbetalinger")
                .associateBy({ UUID.fromString(it.path("id").asText()) as UtbetalingId }) { utbetaling ->
                    val beregningId = UUID.fromString(utbetaling.path("beregningId").asText()) as BeregningId
                    val opprettet = LocalDateTime.parse(utbetaling.path("tidsstempel").asText())
                    beregningTilInnslagId[beregningId] to opprettet
                }

            arbeidsgiver.path("vedtaksperioder").forEach {
                migrerVedtaksperiode(
                    sykefraværstilfeller,
                    sammenhengendePerioder,
                    vilkårsgrunnlag,
                    innslagTidsstempel,
                    utbetalingTilInnslag,
                    infotrygdinntekter,
                    it
                )
            }
            arbeidsgiver.path("forkastede").forEach { forkastet ->
                migrerVedtaksperiode(
                    sykefraværstilfeller,
                    sammenhengendePerioder,
                    vilkårsgrunnlag,
                    innslagTidsstempel,
                    utbetalingTilInnslag,
                    infotrygdinntekter,
                    forkastet.path("vedtaksperiode"),
                    true
                )
            }
        }
    }

    private fun sammenhengendePerioder(arbeidsgiver: JsonNode): List<Periode> {
        val aktive = arbeidsgiver
            .path("vedtaksperioder")
            .mapNotNull { sykdomsperiode(it) }
        val forkastede = arbeidsgiver
                    .path("forkastede")
                    .mapNotNull { sykdomsperiode(it.path("vedtaksperiode")) }
        return (aktive + forkastede).grupperSammenhengendePerioderMedHensynTilHelg()
    }

    private fun sykdomsperiode(vedtaksperiode: JsonNode): Periode? {
        val fom = LocalDate.parse(vedtaksperiode.path("fom").asText())
        val tom = LocalDate.parse(vedtaksperiode.path("tom").asText())
        return fom til tom
    }

    private fun migrerVedtaksperiode(
        sykefraværstilfeller: Set<Sykefraværstilfeller.Sykefraværstilfelle>,
        sammenhengendePerioder: List<Periode>,
        vilkårsgrunnlag: Map<InnslagId, Map<VilkårsgrunnlagId, Vilkårsgrunnlag>>,
        innslagTidsstempel: List<Pair<InnslagId, LocalDateTime>>,
        utbetalingTilInnslag: Map<UtbetalingId, Pair<InnslagId?, LocalDateTime>>,
        infotrygdinntekter: List<Infotrygdhistorikk>,
        vedtaksperiode: JsonNode,
        erForkastet: Boolean = false
    ) {
        val vedtaksperiodeId = UUID.fromString(vedtaksperiode.path("id").asText()) as VedtaksperiodeId
        val skjæringstidspunktVedtaksperiode = LocalDate.parse(vedtaksperiode.path("skjæringstidspunkt").asText())
        val skjæringstidspunktFraInfotrygd = vedtaksperiode.path("skjæringstidspunktFraInfotrygd").takeIf { it.isTextual }?.asText()?.let {
            LocalDate.parse(it)
        }
        val førstedatoVedtaksperiode = LocalDate.parse(vedtaksperiode.path("fom").asText())
        val sistedatoVedtaksperiode = LocalDate.parse(vedtaksperiode.path("tom").asText())
        val søkeperiodeVedtaksperiode = minOf(skjæringstidspunktVedtaksperiode, førstedatoVedtaksperiode, skjæringstidspunktFraInfotrygd ?: førstedatoVedtaksperiode) til sistedatoVedtaksperiode

        val sykefraværstilfelle = sykefraværstilfeller.firstOrNull { tilfelle ->
            tilfelle.periode.overlapperMed(søkeperiodeVedtaksperiode)
        }?.periode ?: søkeperiodeVedtaksperiode

        val søkeperiode = søkeperiodeVedtaksperiode.oppdaterFom(sykefraværstilfelle).let {
            it.oppdaterFom(it.start.minusDays(1))
        }
        val overlappendeSammenhengendePeriode = sammenhengendePerioder.first { it.overlapperMed(søkeperiode) }.let {
            it.oppdaterFom(søkeperiode).oppdaterTom(søkeperiode)
        }.let {
            it.oppdaterFom(it.start.minusDays(1))
        }

        val totalperiode = søkeperiode.merge(sykefraværstilfelle).merge(overlappendeSammenhengendePeriode)

        val tilInfotrygd = vedtaksperiode.path("tilstand").asText() == "TIL_INFOTRYGD"

        val utbetalinger = vedtaksperiode
            .path("utbetalinger")
            .mapNotNull { utbetaling ->
                val vilkårsgrunnlagId = utbetaling.path("vilkårsgrunnlagId").takeUnless { it.isNull || it.isMissingNode }?.asText()
                val utbetalingId = UUID.fromString(utbetaling.path("utbetalingId").asText()) as UtbetalingId
                if (vilkårsgrunnlagId != null) {
                    utbetaling
                } else {
                    var (innslagId, utbetalingOpprettet) = utbetalingTilInnslag.getValue(utbetalingId)

                    if (innslagId == null) {
                        val infotrygdhistorikk = infotrygdinntekter.reversed().firstOrNull { element ->
                            element.tidsstempel <= utbetalingOpprettet && element.inntekter.any { it in totalperiode }
                        }
                        if (infotrygdhistorikk != null) {
                            //
                            innslagId = innslagTidsstempel.lastOrNull { (_, tidsstempel) ->
                                tidsstempel >= infotrygdhistorikk.tidsstempel
                            }?.first
                        }
                    }
                    if (innslagId == null) {
                        // hvis erForkastet er true så kan vi kanskje bare droppe utbetalingen fra listen
                        sikkerlogg.info("[V190] finner ikke vilkårsgrunnlagInnslagId for vedtaksperiodeId=$vedtaksperiodeId erForkastet=$erForkastet, tilInfotrygd=$tilInfotrygd utbetaling=$utbetalingId")
                        if (tilInfotrygd) null
                        else utbetaling
                    } else {
                        val nesteNyeInnslag = innslagTidsstempel.takeWhile { (id, _) ->
                            id != innslagId
                        }.lastOrNull()?.first

                        val match = finnVilkårsgrunnlagForUtbetaling(vilkårsgrunnlag, innslagId, skjæringstidspunktVedtaksperiode, søkeperiode)
                            ?: finnVilkårsgrunnlagForUtbetaling(vilkårsgrunnlag, innslagId, skjæringstidspunktVedtaksperiode, sykefraværstilfelle)
                            ?: finnVilkårsgrunnlagForUtbetaling(vilkårsgrunnlag, innslagId, skjæringstidspunktVedtaksperiode, totalperiode)
                            ?: finnVilkårsgrunnlagForUtbetaling(vilkårsgrunnlag, nesteNyeInnslag, skjæringstidspunktVedtaksperiode, totalperiode)?.also {
                                sikkerlogg.info("[V190] fant match ved å se i neste nye innslag for vedtaksperiode=$vedtaksperiodeId")
                            }
                        match?.log(utbetalingId, vedtaksperiodeId, skjæringstidspunktVedtaksperiode)
                        if (match == null) {
                            sikkerlogg.info("[V190] fant ikke match søkeperioder=[$søkeperiode,$sykefraværstilfelle,$overlappendeSammenhengendePeriode] for utbetaling=$utbetalingId for vedtaksperiode=$vedtaksperiodeId med vedtaksperiodeSkjæringstidspunkt=$skjæringstidspunktVedtaksperiode")
                        } else {
                            (utbetaling as ObjectNode).put("vilkårsgrunnlagId", match.grunnlag.vilkårsgrunnlagId.toString())
                        }
                        utbetaling
                    }
                }
            }

        /* når ikke dry run
        (vedtaksperiode as ObjectNode).replace("utbetalinger", serdeObjectMapper.createArrayNode().apply {
            addAll(utbetalinger)
        })*/
    }

    private fun finnVilkårsgrunnlagForUtbetaling(
        vilkårsgrunnlag: Map<InnslagId, Map<VilkårsgrunnlagId, Vilkårsgrunnlag>>,
        innslagId: InnslagId?,
        skjæringstidspunktVedtaksperiode: LocalDate,
        søkeperiode: Periode
    ): Match? {
        if (innslagId == null) return null
        val ufiltrertListe = vilkårsgrunnlag[innslagId]?.values ?: return null
        val direkteMatch = matchDirekte(ufiltrertListe, skjæringstidspunktVedtaksperiode)
        if (direkteMatch != null) return direkteMatch
        return matchIndirekte(ufiltrertListe, søkeperiode)
    }

    private fun matchDirekte(liste: Collection<Vilkårsgrunnlag>, skjæringstidspunkt: LocalDate): Match? {
        return liste.firstOrNull { grunnlag -> grunnlag.skjæringstidspunkt == skjæringstidspunkt }?.let {
            Match.Direkte(it)
        }
    }
    private fun matchIndirekte(liste: Collection<Vilkårsgrunnlag>, søkeperiode: Periode): Match? {
        return liste.firstOrNull { grunnlag -> grunnlag.skjæringstidspunkt in søkeperiode }?.let {
            Match.Indirekte(søkeperiode, it)
        }
    }

    private sealed class Match(val grunnlag: Vilkårsgrunnlag) {
        abstract fun log(
            utbetalingId: UtbetalingId,
            vedtaksperiodeId: VedtaksperiodeId,
            skjæringstidspunktVedtaksperiode: LocalDate
        )

        protected fun loggMatch(
            tekst: String,
            utbetalingId: UtbetalingId,
            vedtaksperiodeId: VedtaksperiodeId,
            skjæringstidspunktVedtaksperiode: LocalDate
        ) {
            sikkerlogg.info("[V190] $tekst vilkårsgrunnlag=${grunnlag.vilkårsgrunnlagId} skjæringstidspunkt=${grunnlag.skjæringstidspunkt} for utbetaling=$utbetalingId for vedtaksperiode=$vedtaksperiodeId med vedtaksperiodeSkjæringstidspunkt=$skjæringstidspunktVedtaksperiode")
        }

        class Direkte(grunnlag: Vilkårsgrunnlag) : Match(grunnlag) {
            override fun log(
                utbetalingId: UtbetalingId,
                vedtaksperiodeId: VedtaksperiodeId,
                skjæringstidspunktVedtaksperiode: LocalDate
            ) {
                loggMatch("fant direkte match", utbetalingId, vedtaksperiodeId, skjæringstidspunktVedtaksperiode)
            }
        }
        class Indirekte(private val søkeperiode: Periode, grunnlag: Vilkårsgrunnlag) : Match(grunnlag) {
            override fun log(
                utbetalingId: UtbetalingId,
                vedtaksperiodeId: VedtaksperiodeId,
                skjæringstidspunktVedtaksperiode: LocalDate
            ) {
                loggMatch("fant indirekte match i søkeperiode=$søkeperiode", utbetalingId, vedtaksperiodeId, skjæringstidspunktVedtaksperiode)
            }
        }
    }

    private class Vilkårsgrunnlag(
        val innslagId: InnslagId,
        val vilkårsgrunnlagId: VilkårsgrunnlagId,
        val skjæringstidspunkt: LocalDate,
        val fraInfotrygd: Boolean
    )

    private class Infotrygdhistorikk(val tidsstempel: LocalDateTime, val inntekter: List<LocalDate>)
}