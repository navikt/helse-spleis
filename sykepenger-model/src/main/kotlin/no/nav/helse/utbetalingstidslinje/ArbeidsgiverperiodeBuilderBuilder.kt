package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.økonomi.Økonomi

internal class ArbeidsgiverperiodeBuilderBuilder : ArbeidsgiverperiodeMediator {
    private val perioder = mutableListOf<Periode>()
    private val siste get() = perioder.last()

    private val results = mutableListOf<Arbeidsgiverperiode>()
    private var aktivArbeidsgiverperiode: Arbeidsgiverperiode? = null

    internal fun result(): List<Arbeidsgiverperiode> {
        aktivArbeidsgiverperiode?.also { results.add(it) }
        return results.toList()
    }

    override fun arbeidsgiverperiodedag(
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) {
        nyDag(dato)
    }

    override fun arbeidsgiverperiodedagNav(
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) {
        nyDag(dato)
        aktivArbeidsgiverperiode?.utbetalingsdag(dato)
    }

    override fun arbeidsgiverperiodeAvbrutt() {
        reset()
        aktivArbeidsgiverperiode?.also { results.add(it) }
        aktivArbeidsgiverperiode = null
    }

    override fun arbeidsgiverperiodeFerdig() {
        reset()
    }

    override fun fridag(dato: LocalDate) {
        aktivArbeidsgiverperiode?.kjentDag(dato)
    }

    override fun fridagOppholdsdag(dato: LocalDate) {
        aktivArbeidsgiverperiode?.oppholdsdag(dato)
    }

    override fun arbeidsdag(dato: LocalDate) {
        aktivArbeidsgiverperiode?.oppholdsdag(dato)
    }
    override fun utbetalingsdag(dato: LocalDate, økonomi: Økonomi, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        // lager en fiktig arbeidsgiverperiode for Infotrygd-perioder, eller
        // andre tilfeller hvor arbeidsgiverperioden består av 0 dager
        aktivArbeidsgiverperiode?.utbetalingsdag(dato) ?: Arbeidsgiverperiode.fiktiv(dato).also {
            aktivArbeidsgiverperiode = it
        }
    }
    override fun foreldetDag(dato: LocalDate, økonomi: Økonomi) {
        aktivArbeidsgiverperiode?.utbetalingsdag(dato)
    }
    override fun avvistDag(dato: LocalDate, begrunnelse: Begrunnelse, økonomi: Økonomi) {
        aktivArbeidsgiverperiode?.kjentDag(dato)
    }

    private fun nyDag(dagen: LocalDate) {
        if (perioder.isNotEmpty() && siste.endInclusive.plusDays(1) == dagen) {
            perioder[perioder.size - 1] = siste.oppdaterTom(dagen)
        } else {
            perioder.add(dagen.somPeriode())
        }
        val nyArbeidsgiverperiode = Arbeidsgiverperiode(perioder.toList())
        aktivArbeidsgiverperiode?.let { nyArbeidsgiverperiode.kopierMed(it) }
        aktivArbeidsgiverperiode = nyArbeidsgiverperiode
    }

    private fun reset() {
        perioder.clear()
    }

    internal fun build(): Arbeidsgiverperiode? {
        return aktivArbeidsgiverperiode
    }
}
