package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.økonomi.Økonomi

internal interface ArbeidsgiverperiodeMediator {
    // dagen representerer en feriedag som ikke påvirker oppholds-telling ifm arbeidsgiverperioden, enten fordi:
    // det er sykdom på begge kanter av ferien, og dagen er mens vi teller de første 16 dagene, eller:
    // vi har telt 16 dager, og det er sykdom før ferien
    fun fridag(dato: LocalDate)
    // en fridag som teller som påvirker oppholdstellingen; om vi f.eks. har 16 slike etter hverandre så ville
    // vi ha tilbakestilt arbeidsgiverperioden.
    fun fridagOppholdsdag(dato: LocalDate)
    fun arbeidsdag(dato: LocalDate)
    fun arbeidsgiverperiodedag(dato: LocalDate, økonomi: Økonomi, kilde: SykdomstidslinjeHendelse.Hendelseskilde)
    fun arbeidsgiverperiodedagNavAnsvar(dato: LocalDate, økonomi: Økonomi, kilde: SykdomstidslinjeHendelse.Hendelseskilde)
    fun utbetalingsdag(dato: LocalDate, økonomi: Økonomi, kilde: SykdomstidslinjeHendelse.Hendelseskilde)
    fun ukjentDag(dato: LocalDate) {}
    fun foreldetDag(dato: LocalDate, økonomi: Økonomi)
    fun avvistDag(dato: LocalDate, begrunnelse: Begrunnelse)
    fun arbeidsgiverperiodeAvbrutt() {}
    fun arbeidsgiverperiodeFerdig() {}
    fun arbeidsgiverperiodeSistedag() {}
    fun oppholdsdag() {}
}
