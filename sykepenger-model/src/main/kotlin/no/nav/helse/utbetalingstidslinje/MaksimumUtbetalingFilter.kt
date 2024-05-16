package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.etterlevelse.Subsumsjonslogg

internal class MaksimumUtbetalingFilter : UtbetalingstidslinjerFilter {
    override fun filter(
        tidslinjer: List<Utbetalingstidslinje>,
        periode: Periode,
        aktivitetslogg: IAktivitetslogg,
        subsumsjonslogg: Subsumsjonslogg
    ): List<Utbetalingstidslinje> {
        val betalteTidslinjer = Utbetalingstidslinje.betale(tidslinjer)
        val harRedusertUtbetaling = betalteTidslinjer.any { it.er6GBegrenset() }
        if (harRedusertUtbetaling)
            aktivitetslogg.info("Redusert utbetaling minst én dag på grunn av inntekt over 6G")
        else
            aktivitetslogg.info("Utbetaling har ikke blitt redusert på grunn av 6G")
        return betalteTidslinjer
    }

    internal fun betal(
        tidslinjer: List<Utbetalingstidslinje>,
        periode: Periode,
        aktivitetslogg: IAktivitetslogg,
        subsumsjonslogg: Subsumsjonslogg
    ): List<Utbetalingstidslinje> {
        return filter(tidslinjer, periode, aktivitetslogg, subsumsjonslogg)
    }
}
