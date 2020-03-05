package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import java.time.LocalDate

internal class Sykdomsgradfilter(
    private val sykdomsgrader: Sykdomsgrader,
    private val tidslinjer: List<Utbetalingstidslinje>,
    private val periode: Periode,
    private val aktivitetslogg: Aktivitetslogg
) {

    internal fun filter() {
        var harForLavSykdomsgrad = false
        val avvisteDager = mutableListOf<LocalDate>()
        var currentDate = periode.start
        while (!currentDate.isAfter(periode.endInclusive)) {
            if (harForLavSykdomsgrad || sykdomsgrader[currentDate] < 20.0) {
                harForLavSykdomsgrad = true
                avvisteDager.add(currentDate)
            }
            currentDate = currentDate.plusDays(1)
        }
        tidslinjer.forEach { it.avvis(avvisteDager, Begrunnelse.MinimumSykdomsgrad) }
        if (harForLavSykdomsgrad)
            aktivitetslogg.warn("Sykdomsgrad er under 20%% for minst én dag i perioden")
        else
            aktivitetslogg.info("Ingen avviste dager på grunn av 20%% samlet sykdomsgrad-regel")
    }

}
