package no.nav.helse.utbetalingstidslinje

internal interface UtbetalingstidslinjerFilter {
    fun filter(arbeidsgivere: List<Arbeidsgiverberegning>): List<Arbeidsgiverberegning>
}
