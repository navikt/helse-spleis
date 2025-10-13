package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.hendelser.til

internal class Medlemskapsfilter(
    private val erMedlemAvFolketrygden: Boolean
) : UtbetalingstidslinjerFilter {

    override fun filter(arbeidsgivere: List<Arbeidsgiverberegning>): List<Arbeidsgiverberegning> {
        if (erMedlemAvFolketrygden) return arbeidsgivere
        return arbeidsgivere.avvis(listOf(LocalDate.MIN til LocalDate.MAX), Begrunnelse.ManglerMedlemskap)
    }
}
