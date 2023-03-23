package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.Alder
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.etterlevelse.SubsumsjonObserver

internal class ArbeidsgiverUtbetalinger(
    regler: ArbeidsgiverRegler,
    alder: Alder,
    private val arbeidsgivere: Map<Arbeidsgiver, (LocalDate, Periode) -> Utbetalingstidslinje>,
    infotrygdUtbetalingstidslinje: Utbetalingstidslinje,
    vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk
) {
    private val maksimumSykepengedagerfilter = MaksimumSykepengedagerfilter(alder, regler, infotrygdUtbetalingstidslinje)
    private val filtere = listOf(
        Sykdomsgradfilter,
        AvvisDagerEtterDødsdatofilter(alder),
        AvvisInngangsvilkårfilter(vilkårsgrunnlagHistorikk),
        maksimumSykepengedagerfilter,
        MaksimumUtbetalingFilter(),
    )
    internal val maksimumSykepenger by lazy { maksimumSykepengedagerfilter.maksimumSykepenger() }

    internal fun beregn(
        skjæringstidspunkt: LocalDate,
        beregningsperiode: Periode,
        perioder: List<Triple<Periode, IAktivitetslogg, SubsumsjonObserver>>
    ): Pair<Alder.MaksimumSykepenger, Map<Arbeidsgiver, Utbetalingstidslinje>> {
        val tidslinjerPerArbeidsgiver = filtere.fold(tidslinjer(skjæringstidspunkt, beregningsperiode)) { tidslinjer, filter ->
            val input = tidslinjer.entries.map { (key, value) -> key to value }
            val result = filter.filter(input.map { (_, tidslinje) -> tidslinje }, perioder)
            input.zip(result) { (arbeidsgiver, _), utbetalingstidslinje ->
                arbeidsgiver to utbetalingstidslinje
            }.toMap()
        }
        return maksimumSykepenger to tidslinjerPerArbeidsgiver
    }

    private fun tidslinjer(skjæringstidspunkt: LocalDate, periode: Periode) = arbeidsgivere
        .mapValues { (_, builder) -> builder(skjæringstidspunkt, periode) }

}
