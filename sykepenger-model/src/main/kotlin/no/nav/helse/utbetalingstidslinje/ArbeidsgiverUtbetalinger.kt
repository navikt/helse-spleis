package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.IAktivitetslogg
import java.time.LocalDate

internal class ArbeidsgiverUtbetalinger(
    private val regler: ArbeidsgiverRegler,
    private val arbeidsgivere: Map<Arbeidsgiver, IUtbetalingstidslinjeBuilder>,
    private val infotrygdtidslinje: Utbetalingstidslinje,
    private val alder: Alder,
    private val dødsdato: LocalDate?,
) {
    internal lateinit var tidslinjeEngine: MaksimumSykepengedagerfilter

    internal fun beregn(aktivitetslogg : IAktivitetslogg, organisasjonsnummer: String, periode: Periode, virkningsdato: LocalDate = periode.endInclusive) {
        val tidslinjer = arbeidsgivere.mapValues { (arbeidsgiver, builder) ->
            arbeidsgiver.build(builder, periode)
        }
        filtrer(aktivitetslogg, tidslinjer, periode, virkningsdato)
        tidslinjer.forEach { (arbeidsgiver, utbetalingstidslinje) ->
            arbeidsgiver.lagreUtbetalingstidslinjeberegning(organisasjonsnummer, utbetalingstidslinje)
        }
    }

    private fun filtrer(aktivitetslogg: IAktivitetslogg, arbeidsgivere: Map<Arbeidsgiver, Utbetalingstidslinje>, periode: Periode, virkningsdato: LocalDate) {
        val tidslinjer = arbeidsgivere.values.toList()
        Sykdomsgradfilter(tidslinjer, periode, aktivitetslogg).filter()
        AvvisDagerEtterDødsdatofilter(tidslinjer, periode, dødsdato, aktivitetslogg).filter()
        MinimumInntektsfilter(alder, tidslinjer, periode, aktivitetslogg).filter()
        tidslinjeEngine = MaksimumSykepengedagerfilter(alder, regler, periode, aktivitetslogg).also {
            it.filter(tidslinjer, infotrygdtidslinje.kutt(periode.endInclusive))
        }
        MaksimumUtbetaling(tidslinjer, aktivitetslogg, virkningsdato).betal()
    }
}
