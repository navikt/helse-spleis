package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

internal class ArbeidsgiverperiodeBuilderBuilder() : ArbeidsgiverperiodeMediator {
    private val perioder = mutableListOf<Periode>()
    private val siste get() = perioder.last()

    private val results = mutableListOf<Arbeidsgiverperiode>()
    private var aktivArbeidsgiverperiode: Arbeidsgiverperiode? = null

    internal fun result(): List<Arbeidsgiverperiode> {
        reset()
        return results.toList()
    }

    override fun arbeidsgiverperiodedag(dato: LocalDate, økonomi: Økonomi) {
        nyDag(dato)
    }

    override fun arbeidsgiverperiodeAvbrutt() {
        reset()
        aktivArbeidsgiverperiode = null
    }

    override fun arbeidsgiverperiodeFerdig() {
        reset()
    }

    override fun fridag(dato: LocalDate) {
        aktivArbeidsgiverperiode?.kjentDag(dato)
    }
    override fun arbeidsdag(dato: LocalDate) {
        aktivArbeidsgiverperiode?.kjentDag(dato)
    }
    override fun utbetalingsdag(dato: LocalDate, økonomi: Økonomi) {
        // lager en fiktig arbeidsgiverperiode for Infotrygd-perioder, eller
        // andre tilfeller hvor arbeidsgiverperioden består av 0 dager
        aktivArbeidsgiverperiode?.utbetalingsdag(dato) ?: Arbeidsgiverperiode.fiktiv(dato).also {
            results.add(it)
            aktivArbeidsgiverperiode = it
        }
    }
    override fun foreldetDag(dato: LocalDate, økonomi: Økonomi) {
        aktivArbeidsgiverperiode?.kjentDag(dato)
    }
    override fun avvistDag(dato: LocalDate, begrunnelse: Begrunnelse) {
        aktivArbeidsgiverperiode?.kjentDag(dato)
    }

    private fun nyDag(dagen: LocalDate) {
        if (perioder.isNotEmpty() && siste.endInclusive.plusDays(1) == dagen) {
            perioder[perioder.size - 1] = siste.oppdaterTom(dagen)
        } else {
            perioder.add(dagen.somPeriode())
        }
        build()?.also { aktivArbeidsgiverperiode = it }
    }

    private fun reset() {
        build()?.also {
            results.add(it)
            aktivArbeidsgiverperiode = it
        }
        perioder.clear()
    }

    internal fun build(): Arbeidsgiverperiode? {
        if (perioder.isEmpty()) return null
        return Arbeidsgiverperiode(perioder.toList())
    }
}
