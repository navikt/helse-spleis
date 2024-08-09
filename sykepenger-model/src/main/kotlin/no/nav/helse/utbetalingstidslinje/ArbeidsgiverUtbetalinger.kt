package no.nav.helse.utbetalingstidslinje

import no.nav.helse.Alder
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.person.MinimumSykdomsgradsvurdering

internal class ArbeidsgiverUtbetalinger(
    regler: ArbeidsgiverRegler,
    alder: Alder,
    private val arbeidsgivere: (Periode, Subsumsjonslogg, IAktivitetslogg) -> Map<Arbeidsgiver, Utbetalingstidslinje>,
    infotrygdUtbetalingstidslinje: Utbetalingstidslinje,
    vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk,
    minimumSykdomsgradsvurdering: MinimumSykdomsgradsvurdering
) {
    private val maksimumSykepengedagerfilter = MaksimumSykepengedagerfilter(alder, regler, infotrygdUtbetalingstidslinje)
    private val filtere = listOf(
        Sykdomsgradfilter(minimumSykdomsgradsvurdering),
        AvvisDagerEtterDødsdatofilter(alder),
        AvvisInngangsvilkårfilter(vilkårsgrunnlagHistorikk),
        maksimumSykepengedagerfilter,
        MaksimumUtbetalingFilter(),
    )

    internal fun beregn(
        beregningsperiode: Periode,
        beregningsperiodePerArbeidsgiver: Map<Arbeidsgiver, Periode>,
        vedtaksperiode: Periode,
        aktivitetslogg: IAktivitetslogg,
        subsumsjonslogg: Subsumsjonslogg
    ): Map<Arbeidsgiver, Pair<Utbetalingstidslinje, Maksdatosituasjon>> {
        val arbeidsgivertidslinjer = arbeidsgivere(beregningsperiode, subsumsjonslogg, aktivitetslogg)

        val tidslinjerPerArbeidsgiver = filtere.fold(arbeidsgivertidslinjer) { tidslinjer, filter ->
            val input = tidslinjer.entries.map { (key, value) -> key to value }
            val result = filter.filter(input.map { (_, tidslinje) -> tidslinje }, vedtaksperiode, aktivitetslogg, subsumsjonslogg)
            input.zip(result) { (arbeidsgiver, _), utbetalingstidslinje ->
                arbeidsgiver to utbetalingstidslinje
            }.toMap()
        }

        return tidslinjerPerArbeidsgiver.filterKeys { it in beregningsperiodePerArbeidsgiver.keys }.mapValues { (arbeidsgiver, tidslinje) ->
            tidslinje to maksimumSykepengedagerfilter.maksimumSykepenger(beregningsperiodePerArbeidsgiver.getValue(arbeidsgiver), subsumsjonslogg)
        }
    }
}
