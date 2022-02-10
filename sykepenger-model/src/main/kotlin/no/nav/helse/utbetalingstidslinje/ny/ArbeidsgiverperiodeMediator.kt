package no.nav.helse.utbetalingstidslinje.ny

import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

internal interface ArbeidsgiverperiodeMediator {
    fun fridag(dato: LocalDate)
    fun arbeidsdag(dato: LocalDate)
    fun arbeidsgiverperiodedag(dato: LocalDate, økonomi: Økonomi)
    fun utbetalingsdag(dato: LocalDate, økonomi: Økonomi)
    fun arbeidsgiverperiodeAvbrutt() {}
    fun arbeidsgiverperiodeFerdig() {}
}
