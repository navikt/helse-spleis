package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.økonomi.betal
import no.nav.helse.økonomi.er6GBegrenset

internal class MaksimumUtbetalingFilter(
    private val virkningsdato: (periode: Periode) -> LocalDate = { it.endInclusive }
): UtbetalingstidslinjerFilter {
    private var harRedusertUtbetaling = false

    override fun filter(
        tidslinjer: List<Utbetalingstidslinje>,
        periode: Periode,
        aktivitetslogg: IAktivitetslogg,
        subsumsjonObserver: SubsumsjonObserver
    ): List<Utbetalingstidslinje> {
        val virkningsdato = virkningsdato(periode)

        Utbetalingstidslinje.periode(tidslinjer).forEach { dato ->
            tidslinjer.map { it[dato].økonomi }.also { økonomiList ->
                try {
                    økonomiList.betal(virkningsdato)
                    harRedusertUtbetaling = harRedusertUtbetaling || økonomiList.er6GBegrenset()
                } catch (err: Exception) {
                    throw IllegalArgumentException("Klarte ikke å utbetale for dag=$dato, fordi: ${err.message}", err)
                }
            }
        }
        if (harRedusertUtbetaling)
            aktivitetslogg.info("Redusert utbetaling minst én dag på grunn av inntekt over 6G")
        else
            aktivitetslogg.info("Utbetaling har ikke blitt redusert på grunn av 6G")

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
