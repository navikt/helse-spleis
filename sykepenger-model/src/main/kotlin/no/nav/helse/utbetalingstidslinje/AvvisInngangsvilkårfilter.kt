package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til

internal class AvvisInngangsvilkårfilter(
    private val medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus?,
    private val harOpptjening: Boolean
) : UtbetalingstidslinjerFilter {

    override fun filter(
        arbeidsgivere: List<Arbeidsgiverberegning>,
        vedtaksperiode: Periode
    ): List<Arbeidsgiverberegning> {
        return arbeidsgivere
            .avvisMedlemskap()
            .avvisOpptjening()
    }

    private fun List<Arbeidsgiverberegning>.avvisMedlemskap(): List<Arbeidsgiverberegning> {
        if (medlemskapstatus != Medlemskapsvurdering.Medlemskapstatus.Nei) return this
        return avvis(listOf(LocalDate.MIN til LocalDate.MAX), Begrunnelse.ManglerMedlemskap)
    }

    private fun List<Arbeidsgiverberegning>.avvisOpptjening(): List<Arbeidsgiverberegning> {
        if (harOpptjening) return this
        return avvis(listOf(LocalDate.MIN til LocalDate.MAX), Begrunnelse.ManglerOpptjening)
    }
}
