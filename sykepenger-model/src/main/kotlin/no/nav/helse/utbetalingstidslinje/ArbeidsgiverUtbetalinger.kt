package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import java.time.LocalDate

internal class ArbeidsgiverUtbetalinger(
    private val tidslinjer: Map<Arbeidsgiver, Utbetalingstidslinje>,
    private val historiskTidslinje: Utbetalingstidslinje,
    private val periode: Periode,
    private val alder: Alder,
    private val arbeidsgiverRegler: ArbeidsgiverRegler,
    private val aktivitetslogg: Aktivitetslogg
) {
    init {
        require(tidslinjer.size == 1) { "Flere arbeidsgivere er ikke støttet ennå" }
    }

    private var maksdato: LocalDate? = null

    internal fun beregn() {
        val tidslinjer = this.tidslinjer.values.toList()
        val sykdomsgrader = Sykdomsgrader(tidslinjer)
        Sykdomsgradfilter(sykdomsgrader, tidslinjer, periode, aktivitetslogg).filter()
        MinimumInntektsfilter(alder, tidslinjer, periode, aktivitetslogg).filter()
        MaksimumSykepengedagerfilter(alder, arbeidsgiverRegler, periode, aktivitetslogg).also {
            it.filter(tidslinjer, historiskTidslinje)
            maksdato = it.maksdato()
        }
        MaksimumUtbetaling(sykdomsgrader, tidslinjer, periode, aktivitetslogg).beregn()
        this.tidslinjer.forEach { (arbeidsgiver, utbetalingstidslinje) -> arbeidsgiver.push(utbetalingstidslinje) }
    }

    internal fun maksdato() = maksdato
}
