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
        if (dødsdato == null) return
        val avvisteDager = periode.filter { dato -> dato > dødsdato }.takeIf(List<*>::isNotEmpty) ?: return
        if (Utbetalingstidslinje.avvis(tidslinjer, avvisteDager, periode, Begrunnelse.EtterDødsdato))
            return aktivitetslogg.info("Utbetaling stoppet etter $dødsdato grunnet dødsfall")
        aktivitetslogg.info("Personen døde $dødsdato, utenfor den aktuelle perioden.")
    }

}
