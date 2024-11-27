package no.nav.helse.utbetalingstidslinje

import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal class AvvisAndreYtelserFilter(
    private val foreldrepenger: List<Periode>,
    private val svangerskapspenger: List<Periode>,
    private val pleiepenger: List<Periode>,
    private val dagpenger: List<Periode>,
    private val arbeidsavklaringspenger: List<Periode>,
    private val opplæringspenger: List<Periode>,
    private val omsorgspenger: List<Periode>
) : UtbetalingstidslinjerFilter {
    override fun filter(
        tidslinjer: List<Utbetalingstidslinje>,
        periode: Periode,
        aktivitetslogg: IAktivitetslogg,
        subsumsjonslogg: Subsumsjonslogg
    ): List<Utbetalingstidslinje> =
        tidslinjer
            .avvis(foreldrepenger, listOf(Begrunnelse.AndreYtelserForeldrepenger))
            .avvis(svangerskapspenger, listOf(Begrunnelse.AndreYtelserSvangerskapspenger))
            .avvis(pleiepenger, listOf(Begrunnelse.AndreYtelserPleiepenger))
            .avvis(dagpenger, listOf(Begrunnelse.AndreYtelserDagpenger))
            .avvis(arbeidsavklaringspenger, listOf(Begrunnelse.AndreYtelserAap))
            .avvis(opplæringspenger, listOf(Begrunnelse.AndreYtelserOpplaringspenger))
            .avvis(omsorgspenger, listOf(Begrunnelse.AndreYtelserOmsorgspenger))
}
