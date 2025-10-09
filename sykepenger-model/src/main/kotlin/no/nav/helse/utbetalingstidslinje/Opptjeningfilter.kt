package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til

internal class Opptjeningfilter(
    private val harOpptjening: Boolean
) : UtbetalingstidslinjerFilter {

    override fun filter(
        arbeidsgivere: List<Arbeidsgiverberegning>,
        vedtaksperiode: Periode
    ): List<Arbeidsgiverberegning> {
        if (harOpptjening) return arbeidsgivere
        return arbeidsgivere.avvis(listOf(LocalDate.MIN til LocalDate.MAX), Begrunnelse.ManglerOpptjening)
    }
}
