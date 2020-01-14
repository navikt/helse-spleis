package no.nav.helse.utbetalingstidslinje

import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import java.time.LocalDate

internal class ArbeidsgiverUtbetalinger(
    private val tidslinjer: Map<Arbeidsgiver, Utbetalingstidslinje>,
    private val historiskTidslinje: Utbetalingstidslinje,
    private val alder: Alder,
    private val arbeidsgiverRegler: ArbeidsgiverRegler = NormalArbeidstaker
) {
    init {
        require(tidslinjer.size == 1) { "Flere arbeidsgivere støttes ikke enda" }
    }

    private var maksdato: LocalDate? = null

    internal fun beregn() {
        val tidslinjer = this.tidslinjer.values.toList()
        val sykdomsgrader = Sykdomsgrader(tidslinjer)
        Sykdomsgradfilter(sykdomsgrader, tidslinjer).filter()
        MinimumInntektsfilter(alder, tidslinjer).filter()
        MaksimumSykepengedagerfilter(alder, arbeidsgiverRegler).also {
            it.filter(tidslinjer, historiskTidslinje)
            maksdato = it.maksdato()
        }
        MaksimumUtbetaling(sykdomsgrader, tidslinjer).beregn()
        this.tidslinjer.forEach { (arbeidsgiver, utbetalingstidslinje) -> arbeidsgiver.push(utbetalingstidslinje) }
    }

    internal fun maksdato() = maksdato

}
