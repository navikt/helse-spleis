package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.utbetalingslinjer.Utbetaling
import java.time.LocalDate

internal class ArbeidsgiverUtbetalinger(
    private val tidslinjer: Map<Arbeidsgiver, Utbetalingstidslinje>,
    private val historiskTidslinje: Utbetalingstidslinje,
    private val periode: Periode,
    private val alder: Alder,
    private val arbeidsgiverRegler: ArbeidsgiverRegler,
    private val aktivitetslogg: Aktivitetslogg,
    private val organisasjonsnummer: String,
    private val fødselsnummer: String
) {
    init {
        require(tidslinjer.size == 1) { "Flere arbeidsgivere er ikke støttet ennå" }
    }

    private var maksdato: LocalDate? = null
    private var forbrukteSykedager: Int? = null

    internal fun beregn() {
        val tidslinjer = this.tidslinjer.values.toList()
        val sykdomsgrader = Sykdomsgrader(tidslinjer)
        Sykdomsgradfilter(sykdomsgrader, tidslinjer, periode, aktivitetslogg).filter()
        MinimumInntektsfilter(alder, tidslinjer, periode, aktivitetslogg).filter()
        MaksimumSykepengedagerfilter(alder, arbeidsgiverRegler, periode, aktivitetslogg).also {
            it.filter(tidslinjer, historiskTidslinje)
            maksdato = it.maksdato()
            forbrukteSykedager = it.forbrukteSykedager()
        }
        MaksimumUtbetaling(sykdomsgrader, tidslinjer, periode, aktivitetslogg).beregn()
        this.tidslinjer.forEach { (arbeidsgiver, utbetalingstidslinje) ->
            arbeidsgiver.push(Utbetaling(
                fødselsnummer,
                organisasjonsnummer,
                utbetalingstidslinje,
                periode.endInclusive,
                aktivitetslogg,
                arbeidsgiver.utbetaling()
            ))
        }
    }

    internal fun maksdato() = maksdato
    internal fun forbrukteSykedager() = forbrukteSykedager
}
