package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Companion.avvis
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Companion.avvisteDager
import java.time.LocalDate
import no.nav.helse.person.etterlevelse.SubsumsjonObserver

internal class AvvisDagerEtterDødsdatofilter(
    private val dødsdato: LocalDate?,
): UtbetalingstidslinjerFilter {
    private val avvisFra = dødsdato?.plusDays(1) ?: LocalDate.MAX

    override fun filter(
        tidslinjer: List<Utbetalingstidslinje>,
        periode: Periode,
        aktivitetslogg: IAktivitetslogg,
        subsumsjonObserver: SubsumsjonObserver
    ): List<Utbetalingstidslinje> {
        if (dødsdato == null || avvisFra !in periode) return tidslinjer
        avvis(tidslinjer, listOf(avvisFra til LocalDate.MAX), listOf(Begrunnelse.EtterDødsdato))
        if (avvisteDager(tidslinjer, periode, Begrunnelse.EtterDødsdato).isNotEmpty()) aktivitetslogg.info("Utbetaling stoppet etter $dødsdato grunnet dødsfall")
        else aktivitetslogg.info("Personen døde $dødsdato, utenfor den aktuelle perioden.")
        return tidslinjer
    }
}
