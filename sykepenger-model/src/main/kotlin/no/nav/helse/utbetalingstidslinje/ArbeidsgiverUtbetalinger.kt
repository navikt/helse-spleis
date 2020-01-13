package no.nav.helse.utbetalingstidslinje

import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker

internal class ArbeidsgiverUtbetalinger(
    private val tidslinjer: Map<Arbeidsgiver, Utbetalingstidslinje>,
    historiskTidslinje: Utbetalingstidslinje,
    private val alder: Alder,
    private val arbeidsgiverRegler: ArbeidsgiverRegler = NormalArbeidstaker
) {
    init {
        require(tidslinjer.size == 1) { "Flere arbeidsgivere støttes ikke enda" }
    }

    internal fun beregn() {
        val tidslinjer = this.tidslinjer.values.toList()
        val sykdomsgrader = Sykdomsgrader(tidslinjer)
        Sykdomsgradfilter(sykdomsgrader, tidslinjer).filter()
        MinimumInntektsfilter(alder, tidslinjer).filter()
        MaksimumSykepengedagerfilter(alder, arbeidsgiverRegler).filter(tidslinjer)
        MaksimumUtbetaling(sykdomsgrader, tidslinjer).beregn()
        this.tidslinjer.entries.first().apply { key.push(value) }
    }

}
