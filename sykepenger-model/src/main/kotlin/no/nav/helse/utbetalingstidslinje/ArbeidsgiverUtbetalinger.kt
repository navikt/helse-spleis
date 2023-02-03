package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.Alder
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.Vedtaksperiode.Companion.RevurderingUtbetalinger
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.etterlevelse.SubsumsjonObserver

internal class ArbeidsgiverUtbetalinger(
    regler: ArbeidsgiverRegler,
    alder: Alder,
    private val arbeidsgivere: Map<Arbeidsgiver, (Periode) -> Utbetalingstidslinje>,
    infotrygdUtbetalingstidslinje: Utbetalingstidslinje,
    private val dødsdato: LocalDate?,
    private val vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk
) {
    private val maksimumSykepengedagerfilter = MaksimumSykepengedagerfilter(alder, regler, infotrygdUtbetalingstidslinje)
    private val filtere = listOf(
        Sykdomsgradfilter,
        AvvisDagerEtterDødsdatofilter(dødsdato),
        AvvisInngangsvilkårfilter(vilkårsgrunnlagHistorikk),
        maksimumSykepengedagerfilter,
        MaksimumUtbetalingFilter(),
    )
    internal val maksimumSykepenger by lazy { maksimumSykepengedagerfilter.maksimumSykepenger() }

    internal fun beregn(
        organisasjonsnummer: String,
        beregningsperiode: Periode,
        perioder: Map<Periode, Pair<IAktivitetslogg, SubsumsjonObserver>>
    ) {
        val tidslinjerPerArbeidsgiver = tidslinjer(beregningsperiode)
        perioder.forEach { periode, (aktivitetslogg, subsumsjonObserver) ->
            filtrer(aktivitetslogg, tidslinjerPerArbeidsgiver.values.toList(), periode, subsumsjonObserver)
        }
        tidslinjerPerArbeidsgiver.forEach { (arbeidsgiver, utbetalingstidslinje) ->
            arbeidsgiver.lagreUtbetalingstidslinjeberegning(organisasjonsnummer, utbetalingstidslinje, vilkårsgrunnlagHistorikk)
        }
    }

    internal fun utbetal(
        hendelse: IAktivitetslogg,
        revurderingUtbetalinger: RevurderingUtbetalinger,
        beregningsperiode: Periode,
        orgnummer: String,
        utbetalingsperioder: Map<Arbeidsgiver, Vedtaksperiode>
    ) {
        val tidslinjerPerArbeidsgiver = tidslinjer(beregningsperiode)
        revurderingUtbetalinger.filtrer(filtere, tidslinjerPerArbeidsgiver)
        tidslinjerPerArbeidsgiver.forEach { (arbeidsgiver, utbetalingstidslinje) ->
            arbeidsgiver.lagreUtbetalingstidslinjeberegning(orgnummer, utbetalingstidslinje, vilkårsgrunnlagHistorikk)
        }
        tidslinjerPerArbeidsgiver
            .filterValues { utbetalingstidslinje -> utbetalingstidslinje.isNotEmpty() }
            .forEach { (arbeidsgiver, _) ->
                // TODO: kan sende med utbetalingstidslinje for å unngå 'utbetalingstidslinjeberegning'
                utbetalingsperioder[arbeidsgiver]?.lagRevurdering(hendelse.barn(), orgnummer, maksimumSykepenger)
            }
    }

    private fun tidslinjer(periode: Periode) = arbeidsgivere
        .mapValues { (_, builder) -> builder(periode) }

    private fun filtrer(
        aktivitetslogg: IAktivitetslogg,
        tidslinjer: List<Utbetalingstidslinje>,
        periode: Periode,
        subsumsjonObserver: SubsumsjonObserver,
    ) {
        Sykdomsgradfilter.filter(tidslinjer, periode, aktivitetslogg, subsumsjonObserver)
        AvvisDagerEtterDødsdatofilter(dødsdato).filter(tidslinjer, periode, aktivitetslogg, subsumsjonObserver)
        AvvisInngangsvilkårfilter(vilkårsgrunnlagHistorikk).filter(tidslinjer, periode, aktivitetslogg, subsumsjonObserver)
        maksimumSykepengedagerfilter.filter(tidslinjer, periode, aktivitetslogg, subsumsjonObserver)
        MaksimumUtbetalingFilter().betal(tidslinjer, periode, aktivitetslogg, subsumsjonObserver) }
}
