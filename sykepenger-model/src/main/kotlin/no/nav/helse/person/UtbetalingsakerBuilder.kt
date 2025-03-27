package no.nav.helse.person

import java.time.LocalDate
import kotlin.collections.component1
import kotlin.collections.component2
import no.nav.helse.hendelser.Periode
import no.nav.helse.utbetalingslinjer.Utbetalingsak
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverperiodeForVedtaksperiode

internal class UtbetalingsakerBuilder(
    val vedtaksperiodene: List<ArbeidsgiverperiodeForVedtaksperiode>,
    val infotrygdbetalinger: List<Periode>
) {

    /**
     * utbetalingsaker er den logiske inndelingen som binder
     * sammen flere vedtaksperioder til en sak mot Oppdragsystemet
     *
     * i praksis lager vi en utbetalingssak per arbeidsgiverperiode, men vi bryter de opp om
     * det er infotrygdutbetalinger mellom
     */
    fun lagUtbetalingsaker(): List<Utbetalingsak> {
        /* ignorerer infotrygdutbetalinger som overlapper med vedtaksperiodene våre */
        val infotrygdbetalinger = utbetalteInfotrygdperioderMellomVedtaksperioder(vedtaksperiodene)

        return vedtaksperiodene
            .groupBy { finnStartdatoForUtbetalingsakForVedtaksperiode(it, infotrygdbetalinger, vedtaksperiodene) }
            .map { (utbetalingsakensStartdato, perioder) ->
                Utbetalingsak(
                    startperiode = utbetalingsakensStartdato,
                    vedtaksperioder = perioder.map { it.vedtaksperiode }
                )
            }
    }

    private fun finnStartdatoForUtbetalingsakForVedtaksperiode(vedtaksperiode: ArbeidsgiverperiodeForVedtaksperiode, infotrygdbetalinger: List<Periode>, vedtaksperiodene: List<ArbeidsgiverperiodeForVedtaksperiode>): LocalDate {
        // eventuell infotrygdutbetaling som ligger mellom arbeidsgiverperioden og vedtaksperioden
        val mellomliggendeInfotrygdutbetaling = infotrygdutbetalingEtterArbeidsgiverperiodenOgFørVedtaksperioden(infotrygdbetalinger, vedtaksperiode, vedtaksperiodene)
        val infotrygdutbetalingsakStartdato = mellomliggendeInfotrygdutbetaling?.let { infotrygdDag ->
            vedtaksperiodene.first { it.vedtaksperiode.start > infotrygdDag.endInclusive }.vedtaksperiode.start
        }
        /* lar utbetalingsakene starte med enten:
            a) første dag i første vedtaksperiode etter mellomliggende infotrygdutbetaling
            b) første dag i arbeidsgiverperioden
            c) første dag i vedtaksperioden hvis arbeidsgiverperioden er tom og det ikke foreligger noen IT-utbetalinger
         */
        return infotrygdutbetalingsakStartdato
            ?: vedtaksperiode.arbeidsgiverperioder.firstOrNull()?.start
            ?: vedtaksperiode.vedtaksperiode.start
    }

    private fun infotrygdutbetalingEtterArbeidsgiverperiodenOgFørVedtaksperioden(infotrygdbetalinger: List<Periode>, vedtaksperiode: ArbeidsgiverperiodeForVedtaksperiode, vedtaksperiodene: List<ArbeidsgiverperiodeForVedtaksperiode>): Periode? {
        if (vedtaksperiode.arbeidsgiverperioder.isEmpty()) {
            val sisteInfotrygdutbetalingFørVedtaksperiode = infotrygdbetalinger.lastOrNull { infotrygdperiode ->
                infotrygdperiode.endInclusive < vedtaksperiode.vedtaksperiode.start
            } ?: return null

            val gapMellomInfotrygdUtbetalingOgVedtaksperiode = sisteInfotrygdutbetalingFørVedtaksperiode.periodeMellom(vedtaksperiode.vedtaksperiode.start) ?: return sisteInfotrygdutbetalingFørVedtaksperiode
            val andreVedtaksperioderIGapet = vedtaksperiodene.filter { it.vedtaksperiode.overlapperMed(gapMellomInfotrygdUtbetalingOgVedtaksperiode) }

            // Om det finnes vedtaksperioder i gapet _med_ arbeidsgiverperiode kan det umulig være rett å bygge videre på Infotrygd 🤞
            if (andreVedtaksperioderIGapet.any { it.arbeidsgiverperioder.isNotEmpty() }) return null

            // Nå vet vi at alle (om noen) i gapet også er _uten_ arbeidsgiverperiode
            // - Det kan jo være AUU'er, så i perfect storm så kan det fortsatt gå galt ?
            // - Halen på perioden kan jo være arbeidsdager, så egentlig kan "gapet" være større enn vi kan evaluere her
            val sisteVedtaksperiodeSomAlleredeByggerPåInfotrygdutbetaling = andreVedtaksperioderIGapet.lastOrNull()

            val gap = when (sisteVedtaksperiodeSomAlleredeByggerPåInfotrygdutbetaling) {
                null -> gapMellomInfotrygdUtbetalingOgVedtaksperiode.count()
                else -> sisteVedtaksperiodeSomAlleredeByggerPåInfotrygdutbetaling.vedtaksperiode.periodeMellom(vedtaksperiode.vedtaksperiode.start)?.count() ?: 0
            }

            return sisteInfotrygdutbetalingFørVedtaksperiode.takeIf { gap < 16 }
        }

        // Hvorfor kan vi ikke alltid returnere null nå 🤔?
        return infotrygdbetalinger.lastOrNull { infotrygdperiode ->
            infotrygdperiode.start > vedtaksperiode.arbeidsgiverperioder.last().endInclusive &&
            infotrygdperiode.endInclusive < vedtaksperiode.vedtaksperiode.start
        }
    }

    private fun utbetalteInfotrygdperioderMellomVedtaksperioder(vedtaksperiodene: List<ArbeidsgiverperiodeForVedtaksperiode>) =
        infotrygdbetalinger.flatMap { periode ->
            periode.uten(vedtaksperiodene.map { it.vedtaksperiode })
        }
}
