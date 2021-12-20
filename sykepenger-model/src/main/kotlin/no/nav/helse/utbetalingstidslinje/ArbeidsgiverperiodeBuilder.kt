package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

internal class ArbeidsgiverperiodeBuilder internal constructor(regler: ArbeidsgiverRegler = NormalArbeidstaker) : AbstractArbeidsgiverperiodetelling(regler) {
    private val arbeidsgiverperioder = mutableMapOf<Arbeidsgiverperiode, LocalDate>()

    fun result(periode: Periode): Arbeidsgiverperiode? {
        return finnArbeidsgiverperiode(periode)
    }

    private fun finnArbeidsgiverperiode(periode: Periode) =
        arbeidsgiverperioder.firstNotNullOfOrNull { (arbeidsgiverperiode, sisteDag) ->
            arbeidsgiverperiode.takeIf { it.hørerTil(periode, sisteDag) }
        }

    private fun kanskje(dato: LocalDate, arbeidsgiverperiode: Arbeidsgiverperiode?) {
        if (arbeidsgiverperiode == null) return
        arbeidsgiverperioder.compute(arbeidsgiverperiode) { _, _ -> dato }
    }

    override fun sykedagIArbeidsgiverperioden(arbeidsgiverperiode: Arbeidsgiverperiode, dato: LocalDate) {
        arbeidsgiverperioder.remove(arbeidsgiverperiode)
        arbeidsgiverperioder.compute(arbeidsgiverperiode) { _, _ -> dato }
    }

    override fun sykedagEtterArbeidsgiverperioden(arbeidsgiverperiode: Arbeidsgiverperiode?, dato: LocalDate, økonomi: Økonomi) = kanskje(dato, arbeidsgiverperiode)
    override fun sykHelgedagEtterArbeidsgiverperioden(arbeidsgiverperiode: Arbeidsgiverperiode?, dato: LocalDate, økonomi: Økonomi) = kanskje(dato, arbeidsgiverperiode)
    override fun foreldetSykedagEtterArbeidsgiverperioden(arbeidsgiverperiode: Arbeidsgiverperiode?, dato: LocalDate, økonomi: Økonomi) = kanskje(dato, arbeidsgiverperiode)
    override fun egenmeldingsdagEtterArbeidsgiverperioden(arbeidsgiverperiode: Arbeidsgiverperiode?, dato: LocalDate) = kanskje(dato, arbeidsgiverperiode)
    override fun fridagUtenforArbeidsgiverperioden(arbeidsgiverperiode: Arbeidsgiverperiode?, dato: LocalDate) = kanskje(dato, arbeidsgiverperiode)
    override fun arbeidsdag(dato: LocalDate) {}
}
