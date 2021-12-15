package no.nav.helse.utbetalingstidslinje

import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

internal class ArbeidsgiverperiodeBuilder internal constructor(regler: ArbeidsgiverRegler = NormalArbeidstaker) : AbstractArbeidsgiverperiodetelling(regler) {
    private val arbeidsgiverperioder = mutableListOf<Arbeidsgiverperiode>()
    private var pågåendeArbeidsgiverperiode: Arbeidsgiverperiode? = null

    override fun arbeidsgiverperiodeFerdig(arbeidsgiverperiode: Arbeidsgiverperiode, dagen: LocalDate) {
        pågåendeArbeidsgiverperiode = null
        arbeidsgiverperioder.add(arbeidsgiverperiode)
    }

    fun result(): List<Arbeidsgiverperiode> {
        pågåendeArbeidsgiverperiode?.also { arbeidsgiverperioder.add(it) }
        return arbeidsgiverperioder.toList()
    }


    override fun sykedagIArbeidsgiverperioden(arbeidsgiverperiode: Arbeidsgiverperiode, dato: LocalDate) {
        pågåendeArbeidsgiverperiode = arbeidsgiverperiode
    }

    override fun sykedagEtterArbeidsgiverperioden(arbeidsgiverperiode: Arbeidsgiverperiode?, dato: LocalDate, økonomi: Økonomi) {}
    override fun sykHelgedagEtterArbeidsgiverperioden(arbeidsgiverperiode: Arbeidsgiverperiode?, dato: LocalDate, økonomi: Økonomi) {}
    override fun foreldetSykedagEtterArbeidsgiverperioden(arbeidsgiverperiode: Arbeidsgiverperiode?, dato: LocalDate, økonomi: Økonomi) {}
    override fun egenmeldingsdagEtterArbeidsgiverperioden(arbeidsgiverperiode: Arbeidsgiverperiode?, dato: LocalDate) {}
    override fun fridagUtenforArbeidsgiverperioden(arbeidsgiverperiode: Arbeidsgiverperiode?, dato: LocalDate) {}
    override fun arbeidsdag(dato: LocalDate) {}
}
