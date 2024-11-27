package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate

internal interface Arbeidsgiverperiodeoppsamler {
    // dagen representerer en feriedag som ikke påvirker oppholds-telling ifm arbeidsgiverperioden, enten fordi:
    // det er sykdom på begge kanter av ferien, og dagen er mens vi teller de første 16 dagene, eller:
    // vi har telt 16 dager, og det er sykdom før ferien
    fun fridag(dato: LocalDate)

    // en fridag som teller som påvirker oppholdstellingen; om vi f.eks. har 16 slike etter hverandre så ville
    // vi ha tilbakestilt arbeidsgiverperioden.
    fun fridagOppholdsdag(dato: LocalDate)
    fun arbeidsdag(dato: LocalDate)
    fun arbeidsgiverperiodedag(dato: LocalDate)
    fun arbeidsgiverperiodedagNav(dato: LocalDate)
    fun utbetalingsdag(dato: LocalDate)
    fun ukjentDag(dato: LocalDate) {}
    fun foreldetDag(dato: LocalDate)
    fun avvistDag(dato: LocalDate, begrunnelse: Begrunnelse)
    fun arbeidsgiverperiodeAvbrutt() {}
    fun arbeidsgiverperiodeFerdig() {}
    fun arbeidsgiverperiodeSistedag() {}
    fun oppholdsdag() {}
}
