package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.økonomi.betal
import no.nav.helse.økonomi.er6GBegrenset

internal class MaksimumUtbetalingFilter : UtbetalingstidslinjerFilter {
    private var harRedusertUtbetaling = false

    override fun filter(
        tidslinjer: List<Utbetalingstidslinje>,
        perioder: List<Triple<Periode, IAktivitetslogg, SubsumsjonObserver>>
    ): List<Utbetalingstidslinje> {
        Utbetalingstidslinje.periode(tidslinjer).forEach { dato ->
            tidslinjer.map { it[dato].økonomi }.also { økonomiList ->
                try {
                    økonomiList.betal()
                    harRedusertUtbetaling = harRedusertUtbetaling || økonomiList.er6GBegrenset()
                } catch (err: Exception) {
                    throw IllegalArgumentException("Klarte ikke å utbetale for dag=$dato, fordi: ${err.message}", err)
                }
            }
        }
        perioder.forEach { (_, aktivitetslogg, _) ->
            if (harRedusertUtbetaling)
                aktivitetslogg.info("Redusert utbetaling minst én dag på grunn av inntekt over 6G")
            else
                aktivitetslogg.info("Utbetaling har ikke blitt redusert på grunn av 6G")
        }
        return tidslinjer
    }

    internal fun betal(
        tidslinjer: List<Utbetalingstidslinje>,
        periode: Periode,
        aktivitetslogg: IAktivitetslogg,
        subsumsjonObserver: SubsumsjonObserver
    ) {
        filter(tidslinjer, periode, aktivitetslogg, subsumsjonObserver)
    }
}
