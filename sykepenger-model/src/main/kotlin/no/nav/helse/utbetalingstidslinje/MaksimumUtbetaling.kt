package no.nav.helse.utbetalingstidslinje

import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.økonomi.betal
import no.nav.helse.økonomi.er6GBegrenset
import java.time.LocalDate

internal class MaksimumUtbetaling(
    private val tidslinjer: List<Utbetalingstidslinje>,
    private val aktivitetslogg: IAktivitetslogg,
    private val skjæringstidspunkter: List<LocalDate>,
    private val virkningsdato: LocalDate
) {

    private var harRedusertUtbetaling = false

    internal fun betal() {
        Utbetalingstidslinje.periode(tidslinjer).forEach { dato ->
            tidslinjer.map { it[dato].økonomi }.also { økonomiList ->
                økonomiList.betal(skjæringstidspunkt(dato), virkningsdato)
                harRedusertUtbetaling = harRedusertUtbetaling || økonomiList.er6GBegrenset()
            }
        }
        if (harRedusertUtbetaling)
            aktivitetslogg.info("Redusert utbetaling minst én dag på grunn av inntekt over 6G")
        else
            aktivitetslogg.info("Utbetaling har ikke blitt redusert på grunn av 6G")
    }

    private fun skjæringstidspunkt(dato: LocalDate) =
        skjæringstidspunkter
            .filter { dato >= it }
            .maxOrNull()
            ?: dato

}
