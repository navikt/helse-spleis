package no.nav.helse.utbetalingstidslinje

import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.økonomi.betal
import no.nav.helse.økonomi.er6GBegrenset
import java.time.LocalDate

internal class MaksimumUtbetaling(
    private val arbeidsgivere: Map<Arbeidsgiver, Utbetalingstidslinje>,
    private val aktivitetslogg: IAktivitetslogg,
    private val virkningsdato: LocalDate,
    private val skjæringstidspunkter: List<LocalDate>,
) {

    private var harRedusertUtbetaling = false


    internal fun betal() {
        val tidslinjer = arbeidsgivere.values.toList()
        Utbetalingstidslinje.periode(tidslinjer).forEach { dato ->
            arbeidsgivere.map { (arbeidsgiver, tidslinje) -> tidslinje.get2(dato, skjæringstidspunkter, arbeidsgiver).økonomi }.also { økonomiList ->
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
    }
}
