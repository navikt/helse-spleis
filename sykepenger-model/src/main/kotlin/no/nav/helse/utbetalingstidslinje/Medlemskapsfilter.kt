package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til

internal class Medlemskapsfilter(
    private val medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus?
) : UtbetalingstidslinjerFilter {

    override fun filter(
        arbeidsgivere: List<Arbeidsgiverberegning>,
        vedtaksperiode: Periode
    ): List<Arbeidsgiverberegning> {
        if (medlemskapstatus != Medlemskapsvurdering.Medlemskapstatus.Nei) return arbeidsgivere
        return arbeidsgivere.avvis(listOf(LocalDate.MIN til LocalDate.MAX), Begrunnelse.ManglerMedlemskap)
    }
}
