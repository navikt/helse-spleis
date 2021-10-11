package no.nav.helse.utbetalingstidslinje

import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Refusjonshistorikk

internal class Refusjonsgjødsler(
    private val tidslinjer: List<Utbetalingstidslinje>,
    private val refusjonshistorikk: Refusjonshistorikk
) {
    internal fun gjødsle(aktivitetslogg: IAktivitetslogg) {
        tidslinjer.forEach { tidslinje ->
            val refusjon = refusjonshistorikk.finnRefusjon(tidslinje.periode())
            if (refusjon == null) aktivitetslogg.warn("Fant ikke refusjon for perioden. Defaulter til 100%% refusjon. placeholder") // TODO: Spør voksne om tekst
            tidslinje.forEach { utbetalingsdag ->
                when (refusjon) {
                    null -> utbetalingsdag.økonomi.settFullArbeidsgiverRefusjon()
                    else -> utbetalingsdag.økonomi.arbeidsgiverRefusjon(refusjon.beløp(utbetalingsdag.dato, aktivitetslogg))
                }
            }
        }
    }
}
