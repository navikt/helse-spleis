package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.merge
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.økonomi.Økonomi

internal class Sykdomsgradfilter(
    private val tidslinjer: List<Utbetalingstidslinje>,
    private val periode: Periode,
    private val aktivitetslogg: IAktivitetslogg
) {

    internal fun filter() {
        val avvisteDager = Utbetalingstidslinje.periode(tidslinjer).filter { dato ->
            Økonomi.totalSykdomsgrad(tidslinjer.map { it[dato].økonomi }).erUnderGrensen()
        }
        if (Utbetalingstidslinje.avvis(tidslinjer, avvisteDager.merge(), periode, listOf(Begrunnelse.MinimumSykdomsgrad)))
            return aktivitetslogg.warn("Minst én dag uten utbetaling på grunn av sykdomsgrad under 20 %%. Vurder å sende vedtak fra Infotrygd")
        aktivitetslogg.info("Ingen avviste dager på grunn av 20 %% samlet sykdomsgrad-regel for denne perioden")
    }

}
