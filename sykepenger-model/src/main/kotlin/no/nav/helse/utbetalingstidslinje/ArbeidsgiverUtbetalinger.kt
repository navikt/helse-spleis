package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.IAktivitetslogg
import java.time.LocalDate

internal class ArbeidsgiverUtbetalinger(
    private val tidslinjer: Map<Arbeidsgiver, Utbetalingstidslinje>,
    private val personTidslinje: Utbetalingstidslinje,
    private val periode: Periode,
    private val alder: Alder,
    private val arbeidsgiverRegler: ArbeidsgiverRegler,
    private val aktivitetslogg: IAktivitetslogg,
    private val organisasjonsnummer: String,
    private val virkningsdato: LocalDate = periode.endInclusive,
    private val dødsdato: LocalDate? = null
) {
    internal lateinit var tidslinjeEngine: MaksimumSykepengedagerfilter

    internal fun beregn() {
        val tidslinjer = this.tidslinjer.values.toList()
        Sykdomsgradfilter(tidslinjer, periode, aktivitetslogg).filter()
        AvvisDagerEtterDødsdatofilter(tidslinjer, periode, dødsdato, aktivitetslogg).filter()
        MinimumInntektsfilter(alder, tidslinjer, periode, aktivitetslogg).filter()
        tidslinjeEngine = MaksimumSykepengedagerfilter(alder, arbeidsgiverRegler, periode, aktivitetslogg).also {
            it.filter(tidslinjer, personTidslinje)
        }
        MaksimumUtbetaling(tidslinjer, aktivitetslogg, virkningsdato).betal()
        this.tidslinjer.forEach { (arbeidsgiver, utbetalingstidslinje) ->
            arbeidsgiver.lagreUtbetalingstidslinjeberegning(organisasjonsnummer, utbetalingstidslinje)
        }
    }

}
