package no.nav.helse.utbetalingstidslinje

import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.økonomi.Inntekt

internal class MaksimumUtbetalingFilter(
    private val sykepengegrunnlagBegrenset6G: Inntekt,
    private val er6GBegrenset: Boolean
) : UtbetalingstidslinjerFilter {
    override fun filter(
        tidslinjer: List<Utbetalingstidslinje>,
        periode: Periode,
        aktivitetslogg: IAktivitetslogg,
        subsumsjonslogg: Subsumsjonslogg
    ): List<Utbetalingstidslinje> {
        val betalteTidslinjer = Utbetalingstidslinje.betale(sykepengegrunnlagBegrenset6G, tidslinjer)
        if (er6GBegrenset)
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
