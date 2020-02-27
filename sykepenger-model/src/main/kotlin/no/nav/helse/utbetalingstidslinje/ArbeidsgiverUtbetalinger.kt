package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.Arbeidsgiver
import java.time.LocalDate

internal class ArbeidsgiverUtbetalinger(
    private val tidslinjer: Map<Arbeidsgiver, Utbetalingstidslinje>,
    private val historiskTidslinje: Utbetalingstidslinje,
    private val periode: Periode,
    private val alder: Alder,
    private val arbeidsgiverRegler: ArbeidsgiverRegler,
    private val aktivitetslogger: Aktivitetslogger
) {
    init {
        require(tidslinjer.size == 1) { "Flere arbeidsgivere er ikke støttet ennå" }
    }

    private var maksdato: LocalDate? = null
    private var forbrukteSykedager: Int? = null

    internal fun beregn() {
        val tidslinjer = this.tidslinjer.values.toList()
        val sykdomsgrader = Sykdomsgrader(tidslinjer)
        Sykdomsgradfilter(sykdomsgrader, tidslinjer, periode, aktivitetslogger).filter()
        MinimumInntektsfilter(alder, tidslinjer, periode, aktivitetslogger).filter()
        MaksimumSykepengedagerfilter(alder, arbeidsgiverRegler, periode, aktivitetslogger).also {
            it.filter(tidslinjer, historiskTidslinje)
            maksdato = it.maksdato()
            forbrukteSykedager = it.forbrukteSykedager()
        }
        MaksimumUtbetaling(sykdomsgrader, tidslinjer, periode, aktivitetslogger).beregn()
        this.tidslinjer.forEach { (arbeidsgiver, utbetalingstidslinje) -> arbeidsgiver.push(utbetalingstidslinje) }
    }

    internal fun maksdato() = maksdato
    internal fun forbrukteSykedager() = forbrukteSykedager
}
