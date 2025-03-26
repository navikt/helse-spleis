package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode

internal interface UtbetalingstidslinjerFilter {
    fun filter(
        arbeidsgivere: List<Arbeidsgiverberegning>,
        vedtaksperiode: Periode
    ): List<Arbeidsgiverberegning>
}
