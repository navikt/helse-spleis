package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Refusjonshistorikk

internal class Refusjonsgjødsler(
    private val tidslinje: Utbetalingstidslinje,
    private val refusjonshistorikk: Refusjonshistorikk
) {
    internal fun gjødsle(aktivitetslogg: IAktivitetslogg, periode: Periode) {
        sammenhengendeUtbetalingsperioder(tidslinje).forEach { utbetalingsperiode ->
            val refusjon = refusjonshistorikk.finnRefusjon(utbetalingsperiode.periode(), aktivitetslogg)
            if (refusjon == null && utbetalingsperiode.periode().overlapperMed(periode)) {
                aktivitetslogg.warn("Fant ikke refusjonsgrad for perioden. Undersøk oppgitt refusjon før du utbetaler.")
            }

            utbetalingsperiode.forEach { utbetalingsdag ->
                when (refusjon) {
                    null -> utbetalingsdag.økonomi.settFullArbeidsgiverRefusjon()
                    else -> utbetalingsdag.økonomi.arbeidsgiverRefusjon(refusjon.beløp(utbetalingsdag.dato, aktivitetslogg))
                }
            }
        }
    }

    private fun sammenhengendeUtbetalingsperioder(utbetalingstidslinje: Utbetalingstidslinje) = utbetalingstidslinje
        .filter { it !is Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag && it !is Utbetalingstidslinje.Utbetalingsdag.UkjentDag }
        .map(Utbetalingstidslinje.Utbetalingsdag::dato)
        .grupperSammenhengendePerioder()
        .map(utbetalingstidslinje::subset)
        .map(Utbetalingstidslinje::trimLedendeFridager)
        .filter(Utbetalingstidslinje::harUtbetalinger)
}
