package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.IAktivitetslogg
import java.time.LocalDate

internal class AvvisDagerEtterDødsdatofilter(
    private val tidslinjer: List<Utbetalingstidslinje>,
    private val periode: Periode,
    private val dødsdato: LocalDate?,
    private val aktivitetslogg: IAktivitetslogg
) {

    internal fun filter() {
        if (dødsdato == null)
            return
        val avvisteDager = periode.filter { dato ->
            dato.isAfter(dødsdato)
        }
        if (tidslinjer.any { it.avvis(avvisteDager, Begrunnelse.EtterDødsdato) })
            aktivitetslogg.info("Utbetaling stoppet etter $dødsdato grunnet dødsfall")
    }

}
