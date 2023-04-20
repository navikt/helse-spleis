package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.økonomi.betal
import no.nav.helse.økonomi.er6GBegrenset

internal class MaksimumUtbetalingFilter : UtbetalingstidslinjerFilter {
    override fun filter(
        tidslinjer: List<Utbetalingstidslinje>,
        perioder: List<Triple<Periode, IAktivitetslogg, SubsumsjonObserver>>
    ): List<Utbetalingstidslinje> {
        val betalteTidslinjer = Utbetalingstidslinje.betale(tidslinjer)
        val harRedusertUtbetaling = betalteTidslinjer.any { it.er6GBegrenset() }
        perioder.forEach { (_, aktivitetslogg, _) ->
            if (harRedusertUtbetaling)
                aktivitetslogg.info("Redusert utbetaling minst én dag på grunn av inntekt over 6G")
            else
                aktivitetslogg.info("Utbetaling har ikke blitt redusert på grunn av 6G")
        }
        return betalteTidslinjer
    }

    internal fun betal(
        tidslinjer: List<Utbetalingstidslinje>,
        periode: Periode,
        aktivitetslogg: IAktivitetslogg,
        subsumsjonObserver: SubsumsjonObserver
    ): List<Utbetalingstidslinje> {
        return filter(tidslinjer, periode, aktivitetslogg, subsumsjonObserver)
    }
}
