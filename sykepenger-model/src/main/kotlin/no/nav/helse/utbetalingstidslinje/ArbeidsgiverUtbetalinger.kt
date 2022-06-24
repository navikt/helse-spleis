package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk

internal class ArbeidsgiverUtbetalinger(
    private val regler: ArbeidsgiverRegler,
    private val arbeidsgivere: Map<Arbeidsgiver, IUtbetalingstidslinjeBuilder>,
    private val infotrygdhistorikk: Infotrygdhistorikk,
    private val alder: Alder,
    private val dødsdato: LocalDate?,
    private val vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk,
    private val subsumsjonObserver: SubsumsjonObserver
) {
    private lateinit var maksimumSykepengedagerfilter: MaksimumSykepengedagerfilter
    internal val maksimumSykepenger by lazy { maksimumSykepengedagerfilter.maksimumSykepenger() }

    internal fun beregn(
        aktivitetslogg: IAktivitetslogg,
        organisasjonsnummer: String,
        beregningsperiode: Periode,
        perioder: Map<Periode, Pair<IAktivitetslogg, SubsumsjonObserver>>
    ) {
        val tidslinjerPerArbeidsgiver = tidslinjer(beregningsperiode.endInclusive)
        val infotrygdtidslinje = infotrygdhistorikk.utbetalingstidslinje().kutt(beregningsperiode.endInclusive)
        maksimumSykepengedagerfilter = MaksimumSykepengedagerfilter(alder, regler, infotrygdtidslinje)
        gjødsle(aktivitetslogg, beregningsperiode, tidslinjerPerArbeidsgiver)
        perioder.forEach { periode, (aktivitetslogg, subsumsjonObserver) ->
            filtrer(aktivitetslogg, tidslinjerPerArbeidsgiver.values.toList(), periode, subsumsjonObserver)
        }
        tidslinjerPerArbeidsgiver.forEach { (arbeidsgiver, utbetalingstidslinje) ->
            arbeidsgiver.lagreUtbetalingstidslinjeberegning(organisasjonsnummer, utbetalingstidslinje, vilkårsgrunnlagHistorikk)
        }
    }

    private fun tidslinjer(kuttdato: LocalDate) = arbeidsgivere
        .mapValues { (arbeidsgiver, builder) -> arbeidsgiver.build(subsumsjonObserver, infotrygdhistorikk, builder, kuttdato) }
        .filterValues { it.isNotEmpty() }

    private fun gjødsle(aktivitetslogg: IAktivitetslogg, periode: Periode, arbeidsgivere: Map<Arbeidsgiver, Utbetalingstidslinje>) {
        arbeidsgivere.forEach { (arbeidsgiver, tidslinje) ->
            Refusjonsgjødsler(
                tidslinje = tidslinje + arbeidsgiver.utbetalingstidslinje(infotrygdhistorikk),
                refusjonshistorikk = arbeidsgiver.refusjonshistorikk,
                infotrygdhistorikk = infotrygdhistorikk,
                organisasjonsnummer = arbeidsgiver.organisasjonsnummer()
            ).gjødsle(aktivitetslogg, periode)
        }
    }

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
        MaksimumUtbetalingFilter { periode.endInclusive }.betal(tidslinjer, periode, aktivitetslogg, subsumsjonObserver) }
}
