package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.økonomi.Økonomi

internal interface ArbeidsgiverperiodeMediator {
    fun fridag(dato: LocalDate)
    fun arbeidsdag(dato: LocalDate)
    fun arbeidsgiverperiodedag(dato: LocalDate, økonomi: Økonomi, kilde: SykdomstidslinjeHendelse.Hendelseskilde)
    fun utbetalingsdag(dato: LocalDate, økonomi: Økonomi, kilde: SykdomstidslinjeHendelse.Hendelseskilde)
    fun ukjentDag(dato: LocalDate) {}
    fun foreldetDag(dato: LocalDate, økonomi: Økonomi)
    fun avvistDag(dato: LocalDate, begrunnelse: Begrunnelse)
    fun arbeidsgiverperiodeAvbrutt() {}
    fun arbeidsgiverperiodeFerdig() {}
    fun arbeidsgiverperiodeSistedag() {}
    fun oppholdsdag() {}
}
