package no.nav.helse.utbetalingstidslinje.ny

import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

internal class UtbetalingstidslinjeBuilder() : ArbeidsgiverperiodeMediator {
    private val tidslinje = Utbetalingstidslinje()
    private val periodebuilder = ArbeidsgiverperiodeBuilderBuilder()
    private var sisteArbeidsgiverperiode: Arbeidsgiverperiode? = null
    private val nåværendeArbeidsgiverperiode: Arbeidsgiverperiode? get() = sisteArbeidsgiverperiode ?: periodebuilder.build()

    internal fun result(): Utbetalingstidslinje {
        return tidslinje
    }

    override fun fridag(dato: LocalDate) {
        tidslinje.addFridag(dato, Økonomi.ikkeBetalt(nåværendeArbeidsgiverperiode))
    }

    override fun arbeidsdag(dato: LocalDate) {
        tidslinje.addArbeidsdag(dato, Økonomi.ikkeBetalt(nåværendeArbeidsgiverperiode))
    }

    override fun arbeidsgiverperiodedag(dato: LocalDate, økonomi: Økonomi) {
        periodebuilder.arbeidsgiverperiodedag(dato, økonomi)
        tidslinje.addArbeidsgiverperiodedag(dato, Økonomi.ikkeBetalt(nåværendeArbeidsgiverperiode))
    }

    override fun utbetalingsdag(dato: LocalDate, økonomi: Økonomi) {
        if (dato.erHelg()) return tidslinje.addHelg(dato, økonomi.inntekt(
            aktuellDagsinntekt = Inntekt.INGEN,
            dekningsgrunnlag = Inntekt.INGEN,
            skjæringstidspunkt = dato,
            arbeidsgiverperiode = nåværendeArbeidsgiverperiode
        ))
        tidslinje.addNAVdag(dato, økonomi.inntekt(
            aktuellDagsinntekt = 1000.daglig,
            dekningsgrunnlag = 1000.daglig,
            skjæringstidspunkt = dato,
            arbeidsgiverperiode = nåværendeArbeidsgiverperiode
        ))
    }

    override fun arbeidsgiverperiodeAvbrutt() {
        periodebuilder.arbeidsgiverperiodeAvbrutt()
        sisteArbeidsgiverperiode = null
    }

    override fun arbeidsgiverperiodeFerdig() {
        periodebuilder.arbeidsgiverperiodeFerdig()
        sisteArbeidsgiverperiode = periodebuilder.result().lastOrNull()
    }
}
