package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Refusjonshistorikk
import no.nav.helse.person.Varselkode.RV_RE_1

internal class Refusjonsgjødsler(
    private val tidslinje: Utbetalingstidslinje,
    private val refusjonshistorikk: Refusjonshistorikk
) {
    internal fun gjødsle(aktivitetslogg: IAktivitetslogg, periode: Periode) {
        tidslinje.sammenhengendeUtbetalingsperioder().forEach { utbetalingsperiode ->
            val refusjon = refusjonshistorikk.finnRefusjon(utbetalingsperiode.periode(), aktivitetslogg)
            if (refusjon == null && utbetalingsperiode.periode().overlapperMed(periode)) aktivitetslogg.varsel(RV_RE_1)
            utbetalingsperiode.forEach { utbetalingsdag -> utbetalingsdag.gjødsle(refusjon) }
        }
    }
}
