package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.økonomi.Økonomi

internal class Sykdomsgradfilter(
    private val tidslinjer: List<Utbetalingstidslinje>,
    private val periode: Periode,
    private val aktivitetslogg: Aktivitetslogg
) {

    internal fun filter() {
        val avvisteDager = periode.filter{dato ->
            Økonomi.sykdomsgrad(tidslinjer.map { it[dato].økonomi }).erUnderGrensen()
        }
        if (tidslinjer.any { it.avvis(avvisteDager, Begrunnelse.MinimumSykdomsgrad) })
            aktivitetslogg.warn("Minst én dag uten utbetaling på grunn av sykdomsgrad under 20 %%. Vurder å sende brev")
        else
            aktivitetslogg.info("Ingen avviste dager på grunn av 20 %% samlet sykdomsgrad-regel")
    }

}
