package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

internal class Sykdomsgradfilter(
    private val sykdomsgrader: Sykdomsgrader,
    private val tidslinjer: List<Utbetalingstidslinje>,
    private val periode: Periode,
    private val aktivitetslogg: Aktivitetslogg
) {

    internal fun filter() {
        val avvisteDager = periode.filter{dato ->
            Økonomi.sykdomsgrad(tidslinjer.map { it[dato].økonomi }).erUnderGrensen()
        }
        tidslinjer.forEach { it.avvis(avvisteDager, Begrunnelse.MinimumSykdomsgrad) }
        if (avvisteDager.isEmpty())
            aktivitetslogg.info("Ingen avviste dager på grunn av 20%% samlet sykdomsgrad-regel")
        else
            aktivitetslogg.warn("Minst én dag uten utbetaling på grunn av sykdomsgrad under 20%%")
    }

}
