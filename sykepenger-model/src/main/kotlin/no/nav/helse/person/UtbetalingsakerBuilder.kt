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
        val mellomliggendeInfotrygdutbetaling = infotrygdutbetalingEtterArbeidsgiverperiodenOgFørVedtaksperioden(infotrygdbetalinger, vedtaksperiode)
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

    private fun infotrygdutbetalingEtterArbeidsgiverperiodenOgFørVedtaksperioden(infotrygdbetalinger: List<Periode>, vedtaksperiode: ArbeidsgiverperiodeForVedtaksperiode): Periode? {
        if (vedtaksperiode.førsteDagUtbetaltIInfotrygd()) return null
        return infotrygdbetalinger.lastOrNull { infotrygdperiode ->
            (vedtaksperiode.arbeidsgiverperioder.isEmpty() || infotrygdperiode.start > vedtaksperiode.arbeidsgiverperioder.last().endInclusive)
                && infotrygdperiode.endInclusive < vedtaksperiode.vedtaksperiode.start
        }
    }

    /**
     * Når arbeidsgiverperioden er en tom liste skal det være ensbetydende med at arbeidsgiverperioden
     * er gjennomført i Infotrygd, og vi ikke skal påstarte en ny telling av arbeisdgiverperioden i Spleis.
     * Da utbetales det fra dag én i Spleis-perioden.
     *
     * Når infotrygdutbetalingen ligger i forkant av Spleis-perioden så klarer vi å hekte oss på riktig utbetalingssak.
     * - Det er situasjoner hvor infotrygdutbetalingen ligger med et gap på maks 15 dager til Spleis-perioden.
     *
     * I spesialtilfellet hvor vi har en periode i Spleis som blir utbetalt fra dag én i Infotrygd så
     * har vi også en tom liste med arbeidsgiverperiode.
     * Da vet vi ikke om det utelukkende er på grunn av at dag én er utbetalt i Infotrygd, eller om det også er utbetalt i
     * Infotrygd i forkant (med maks 15 dagers gap ☝) slik at det uansett ville blitt en tom liste med arbeidsgiverperiode.
     *
     * Når det ikke er utbetalt i forkant i Infotrygd, og det kun er utbetalingen av dag én i Infotrygd som forårsaker
     * at vi har en tom liste med arbeidsgiverperiode leter "algoritmen" som skal finne rett utbetalingssak uansett etter
     * Infotrygdutbetalinger _før_ vedtaksperioden.
     * Da kan det være at denne utbetalingen i Infotrygd er flere år gammel, og vi antar den er relevant for vår
     * utbetalingssak. Da blir eventuell mellomliggende perioder utbetalt i Spleis annullert uten at de tilhørende
     * vedtaksperiodene blir revurdert, eller det er veldig tydlig for hverken saksbehandler eller oss.
     *
     * For å unngå å havne i den situasjonen velger vi heller å starte en ny utbetalingssak i dette spesialtilfellet.
     * Da blir beløpene som utbetales rett, men vi kan ende opp med fler utbetalingssaker innenfor én arbeidsgiverperiode
     * hvor det ideelt sett skulle vært én. Det er mye mindre konsekvenser ved å godta den "feilen" fremfor
     * feilsituasjonen som beskrevet ovenfor.
     *
     */
    private fun ArbeidsgiverperiodeForVedtaksperiode.førsteDagUtbetaltIInfotrygd(): Boolean {
        if (arbeidsgiverperioder.isNotEmpty()) return false
        return infotrygdbetalinger.any { vedtaksperiode.start in it }
    }

    private fun utbetalteInfotrygdperioderMellomVedtaksperioder(vedtaksperiodene: List<ArbeidsgiverperiodeForVedtaksperiode>) =
        infotrygdbetalinger.flatMap { periode ->
            periode.uten(vedtaksperiodene.map { it.vedtaksperiode })
        }
}
