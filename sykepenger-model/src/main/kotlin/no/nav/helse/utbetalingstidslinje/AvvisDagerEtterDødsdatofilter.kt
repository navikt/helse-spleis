package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Companion.avvisteDager
import no.nav.helse.Alder
import no.nav.helse.etterlevelse.Subsumsjonslogg

internal class AvvisDagerEtterDødsdatofilter(
    private val alder: Alder,
): UtbetalingstidslinjerFilter {

    override fun filter(
        tidslinjer: List<Utbetalingstidslinje>,
        periode: Periode,
        aktivitetslogg: IAktivitetslogg,
        subsumsjonslogg: Subsumsjonslogg
    ): List<Utbetalingstidslinje> {
        val avvisteTidslinjer = alder.avvisDager(tidslinjer)

        val avvisteDager = avvisteDager(avvisteTidslinjer, periode, Begrunnelse.EtterDødsdato)
        if (avvisteDager.isNotEmpty()) aktivitetslogg.info("Utbetaling stoppet etter ${avvisteDager.first().dato} grunnet dødsfall")

        return avvisteTidslinjer
    }
}
