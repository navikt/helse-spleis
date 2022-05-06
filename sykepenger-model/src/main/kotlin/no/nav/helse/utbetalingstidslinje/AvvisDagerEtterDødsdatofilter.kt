package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Companion.avvis
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Companion.avvisteDager
import java.time.LocalDate

internal interface IAvvisDagerEtterDødsdatofilter {
    fun filter(
        tidslinjer: List<Utbetalingstidslinje>,
        periode: Periode,
        aktivitetslogg: IAktivitetslogg
    )
}

internal class AvvisDagerEtterDødsdatofilter(
    private val dødsdato: LocalDate?,
): IAvvisDagerEtterDødsdatofilter {
    private val avvisFra = dødsdato?.plusDays(1) ?: LocalDate.MAX
    override fun filter(
        tidslinjer: List<Utbetalingstidslinje>,
        periode: Periode,
        aktivitetslogg: IAktivitetslogg
    ) {
        if (dødsdato == null || avvisFra !in periode) return
        avvis(tidslinjer, listOf(avvisFra til LocalDate.MAX), listOf(Begrunnelse.EtterDødsdato))
        if (avvisteDager(tidslinjer, periode, Begrunnelse.EtterDødsdato).isNotEmpty())
            return aktivitetslogg.info("Utbetaling stoppet etter $dødsdato grunnet dødsfall")
        aktivitetslogg.info("Personen døde $dødsdato, utenfor den aktuelle perioden.")
    }
}
