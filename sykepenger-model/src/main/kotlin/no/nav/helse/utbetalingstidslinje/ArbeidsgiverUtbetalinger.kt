package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import java.time.LocalDate

internal class ArbeidsgiverUtbetalinger(
    private val tidslinjer: Map<Arbeidsgiver, Utbetalingstidslinje>,
    private val personTidslinje: Utbetalingstidslinje,
    private val periode: Periode,
    private val alder: Alder,
    private val arbeidsgiverRegler: ArbeidsgiverRegler,
    private val aktivitetslogg: Aktivitetslogg,
    private val organisasjonsnummer: String,
    private val fødselsnummer: String
) {
    private var maksdato: LocalDate? = null
    private var gjenståendeSykedager: Int? = null
    private var forbrukteSykedager: Int? = null
    private lateinit var tidslinjeEngine: MaksimumSykepengedagerfilter

    internal fun beregn() {
        val tidslinjer = this.tidslinjer.values.toList()
        val sykdomsgrader = Sykdomsgrader(tidslinjer)
        Sykdomsgradfilter(sykdomsgrader, tidslinjer, periode, aktivitetslogg).filter()
        MinimumInntektsfilter(alder, tidslinjer, periode, aktivitetslogg).filter()
        tidslinjeEngine = MaksimumSykepengedagerfilter(alder, arbeidsgiverRegler, periode, aktivitetslogg).also {
            it.filter(tidslinjer, personTidslinje)
        }
        MaksimumUtbetaling(tidslinjer, aktivitetslogg).betal()
        this.tidslinjer.forEach { (arbeidsgiver, utbetalingstidslinje) ->
            arbeidsgiver.createUtbetaling(
                fødselsnummer,
                organisasjonsnummer,
                utbetalingstidslinje,
                periode.endInclusive,
                aktivitetslogg
            )
        }
    }

    internal fun beregnGrenser(sisteDato: LocalDate) {
        tidslinjeEngine.beregnGrenser(sisteDato)
        maksdato = tidslinjeEngine.maksdato()
        gjenståendeSykedager = tidslinjeEngine.gjenståendeSykedager()
        forbrukteSykedager = tidslinjeEngine.forbrukteSykedager()
    }

    internal fun maksdato() = maksdato
    internal fun gjenståendeSykedager() = gjenståendeSykedager
    internal fun forbrukteSykedager() = forbrukteSykedager
}
