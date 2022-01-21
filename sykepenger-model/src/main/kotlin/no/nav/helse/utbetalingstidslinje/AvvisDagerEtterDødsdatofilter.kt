package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Companion.avvis
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Companion.avvisteDager
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
        avvis(tidslinjer, listOf(avvisFra til LocalDate.MAX), listOf(Begrunnelse.EtterDødsdato))
        if (avvisteDager(tidslinjer, periode, Begrunnelse.EtterDødsdato).isNotEmpty())
            return aktivitetslogg.info("Utbetaling stoppet etter $dødsdato grunnet dødsfall")
        aktivitetslogg.info("Personen døde $dødsdato, utenfor den aktuelle perioden.")
    }

}
