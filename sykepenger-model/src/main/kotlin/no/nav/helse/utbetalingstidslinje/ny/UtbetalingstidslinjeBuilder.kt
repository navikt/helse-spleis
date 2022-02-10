package no.nav.helse.utbetalingstidslinje.ny

import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

internal class UtbetalingstidslinjeBuilder(private val inntekter: Inntekter) : ArbeidsgiverperiodeMediator {
    private val tidslinje = Utbetalingstidslinje()
    private val periodebuilder = ArbeidsgiverperiodeBuilderBuilder()
    private var sisteArbeidsgiverperiode: Arbeidsgiverperiode? = null
    private val nåværendeArbeidsgiverperiode: Arbeidsgiverperiode? get() = sisteArbeidsgiverperiode ?: periodebuilder.build()

    internal fun result(): Utbetalingstidslinje {
        return tidslinje
    }

    override fun fridag(dato: LocalDate) {
        tidslinje.addFridag(dato, inntekter.medFrivilligInntekt(dato, Økonomi.ikkeBetalt(nåværendeArbeidsgiverperiode)))
    }

    override fun arbeidsdag(dato: LocalDate) {
        tidslinje.addArbeidsdag(dato, inntekter.medFrivilligInntekt(dato, Økonomi.ikkeBetalt(nåværendeArbeidsgiverperiode)))
    }

    override fun arbeidsgiverperiodedag(dato: LocalDate, økonomi: Økonomi) {
        periodebuilder.arbeidsgiverperiodedag(dato, økonomi)
        tidslinje.addArbeidsgiverperiodedag(dato, inntekter.medFrivilligInntekt(dato, Økonomi.ikkeBetalt(nåværendeArbeidsgiverperiode)))
    }

    override fun utbetalingsdag(dato: LocalDate, økonomi: Økonomi) {
        if (dato.erHelg()) return tidslinje.addHelg(dato, inntekter.medSkjæringstidspunkt(dato, økonomi, nåværendeArbeidsgiverperiode))
        tidslinje.addNAVdag(dato, inntekter.medInntekt(dato, økonomi, nåværendeArbeidsgiverperiode))
    }

    override fun foreldetDag(dato: LocalDate, økonomi: Økonomi) {
        tidslinje.addForeldetDag(dato, inntekter.medInntekt(dato, økonomi, nåværendeArbeidsgiverperiode))
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
