package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.IAktivitetslogg
import java.time.LocalDate

internal class AvvisDagerEtterDødsdatofilter(
    private val tidslinjer: List<Utbetalingstidslinje>,
    private val periode: Periode,
    private val dødsdato: LocalDate?,
    private val aktivitetslogg: IAktivitetslogg
) {
    private val avvisFra = dødsdato?.plusDays(1) ?: LocalDate.MAX
    internal fun filter() {
        if (dødsdato == null || avvisFra !in periode) return
        if (Utbetalingstidslinje.avvis(tidslinjer, avvisFra til LocalDate.MAX, periode, listOf(Begrunnelse.EtterDødsdato)))
            return aktivitetslogg.info("Utbetaling stoppet etter $dødsdato grunnet dødsfall")
        aktivitetslogg.info("Personen døde $dødsdato, utenfor den aktuelle perioden.")
    }

}
