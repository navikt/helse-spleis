package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Etterlevelse.Vurderingsresultat.Companion.`§8-3 ledd 1`
import no.nav.helse.person.IAktivitetslogg
import java.time.LocalDate

internal class AvvisDagerEtterFylte70ÅrFilter(
    private val tidslinjer: List<Utbetalingstidslinje>,
    private val periode: Periode,
    alder: Alder,
    private val aktivitetslogg: IAktivitetslogg
) {

    private val syttiårsdagen = alder.søttiårsdagen

    internal fun filter() {
        if (Utbetalingstidslinje.avvis(tidslinjer, syttiårsdagen til LocalDate.MAX, periode, listOf(Begrunnelse.Over70))) {
            aktivitetslogg.info("Utbetaling stoppet etter $syttiårsdagen, søker fylte 70 år.")
            aktivitetslogg.`§8-3 ledd 1`(false)
        }
    }

}
