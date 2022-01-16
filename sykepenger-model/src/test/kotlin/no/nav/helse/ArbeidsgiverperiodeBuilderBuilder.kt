package no.nav.helse

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

internal class ArbeidsgiverperiodeBuilderBuilder() : ArbeidsgiverperiodeMediator {
    private val perioder = mutableListOf<Periode>()
    private val siste get() = perioder.last()

    private val results = mutableListOf<Arbeidsgiverperiode>()

    internal fun result(): List<Arbeidsgiverperiode> {
        reset()
        return results.toList()
    }

    override fun arbeidsgiverperiodedag(dato: LocalDate, økonomi: Økonomi) {
        nyDag(dato)
    }

    override fun arbeidsgiverperiodeAvbrutt() {
        reset()
    }

    override fun arbeidsgiverperiodeFerdig() {
        reset()
    }

    override fun fridag(dato: LocalDate) {}
    override fun arbeidsdag(dato: LocalDate) {}
    override fun utbetalingsdag(dato: LocalDate, økonomi: Økonomi) {}

    private fun nyDag(dagen: LocalDate) {
        if (perioder.isNotEmpty() && siste.endInclusive.plusDays(1) == dagen) {
            perioder[perioder.size - 1] = siste.oppdaterTom(dagen)
        } else {
            perioder.add(dagen.somPeriode())
        }
    }

    private fun reset() {
        if (perioder.isEmpty()) return
        results.add(build())
        perioder.clear()
    }

    private fun build(): Arbeidsgiverperiode {
        return Arbeidsgiverperiode(perioder.toList())
    }
}
