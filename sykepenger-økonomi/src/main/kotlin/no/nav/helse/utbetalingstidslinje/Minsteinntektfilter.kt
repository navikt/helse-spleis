package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.hendelser.til
import no.nav.helse.nesteDag

internal class Minsteinntektfilter(
    private val sekstisyvårsdagen: LocalDate,
    private val erUnderMinsteinntektskravTilFylte67: Boolean,
    private val erUnderMinsteinntektEtterFylte67: Boolean
) : UtbetalingstidslinjerFilter {

    override fun filter(arbeidsgivere: List<Arbeidsgiverberegning>): List<Arbeidsgiverberegning> {
        return arbeidsgivere
            .avvisMinsteinntektTilFylte67()
            .avvisMinsteinntektEtterFylte67()
    }

    private fun List<Arbeidsgiverberegning>.avvisMinsteinntektTilFylte67(): List<Arbeidsgiverberegning> {
        if (!erUnderMinsteinntektskravTilFylte67) return this
        return avvis(listOf(LocalDate.MIN til sekstisyvårsdagen), Begrunnelse.MinimumInntekt)
    }

    private fun List<Arbeidsgiverberegning>.avvisMinsteinntektEtterFylte67(): List<Arbeidsgiverberegning> {
        if (!erUnderMinsteinntektEtterFylte67) return this
        return avvis(listOf(sekstisyvårsdagen.nesteDag til LocalDate.MAX), Begrunnelse.MinimumInntektOver67)
    }
}
