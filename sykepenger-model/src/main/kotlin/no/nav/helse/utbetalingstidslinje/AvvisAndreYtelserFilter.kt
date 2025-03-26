package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode

internal class AvvisAndreYtelserFilter(
    private val foreldrepenger: List<Periode>,
    private val svangerskapspenger: List<Periode>,
    private val pleiepenger: List<Periode>,
    private val dagpenger: List<Periode>,
    private val arbeidsavklaringspenger: List<Periode>,
    private val opplæringspenger: List<Periode>,
    private val omsorgspenger: List<Periode>,
) : UtbetalingstidslinjerFilter {

    override fun filter(
        arbeidsgivere: List<Arbeidsgiverberegning>,
        vedtaksperiode: Periode
    ): List<Arbeidsgiverberegning> {
        return arbeidsgivere
            .avvis(foreldrepenger, Begrunnelse.AndreYtelserForeldrepenger)
            .avvis(svangerskapspenger, Begrunnelse.AndreYtelserSvangerskapspenger)
            .avvis(pleiepenger, Begrunnelse.AndreYtelserPleiepenger)
            .avvis(dagpenger, Begrunnelse.AndreYtelserDagpenger)
            .avvis(arbeidsavklaringspenger, Begrunnelse.AndreYtelserAap)
            .avvis(opplæringspenger, Begrunnelse.AndreYtelserOpplaringspenger)
            .avvis(omsorgspenger, Begrunnelse.AndreYtelserOmsorgspenger)
    }
}
