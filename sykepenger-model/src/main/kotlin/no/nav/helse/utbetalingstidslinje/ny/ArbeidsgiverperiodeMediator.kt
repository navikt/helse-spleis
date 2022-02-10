package no.nav.helse.utbetalingstidslinje.ny

import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

internal interface ArbeidsgiverperiodeMediator {
    fun fridag(dato: LocalDate)
    fun arbeidsdag(dato: LocalDate)
    fun arbeidsgiverperiodedag(dato: LocalDate, økonomi: Økonomi)
    fun utbetalingsdag(dato: LocalDate, økonomi: Økonomi)
    fun foreldetDag(dato: LocalDate, økonomi: Økonomi)
    fun avvistDag(dato: LocalDate, begrunnelse: Begrunnelse)
    fun arbeidsgiverperiodeAvbrutt() {}
    fun arbeidsgiverperiodeFerdig() {}
}
