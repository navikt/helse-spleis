package no.nav.helse

import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

internal class UtbetalingstidslinjeBuilder() : ArbeidsgiverperiodeMediator {
    private val tidslinje = Utbetalingstidslinje()

    internal fun result(): Utbetalingstidslinje {
        return tidslinje
    }

    override fun fridag(dato: LocalDate) {
        tidslinje.addFridag(dato, Økonomi.ikkeBetalt())
    }

    override fun arbeidsdag(dato: LocalDate) {
        tidslinje.addArbeidsdag(dato, Økonomi.ikkeBetalt())
    }

    override fun arbeidsgiverperiodedag(dato: LocalDate, økonomi: Økonomi) {
        tidslinje.addArbeidsgiverperiodedag(dato, økonomi)
    }

    override fun utbetalingsdag(dato: LocalDate, økonomi: Økonomi) {
        if (dato.erHelg()) tidslinje.addHelg(dato, økonomi)
        else tidslinje.addNAVdag(dato, økonomi)
    }
}
