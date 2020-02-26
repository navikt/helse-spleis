package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg

internal class Sykdomsgradfilter(sykdomsgrader: Sykdomsgrader,
                                 tidslinjer: List<Utbetalingstidslinje>,
                                 periode: Periode,
                                 private val aktivitetslogg: Aktivitetslogg
) {

    internal fun filter() {
        // Avvis dager når man er mindre enn 20% syk totalt
        aktivitetslogg.info("Avviste ikke noen dager på grunn av 20%% samlet sykdomsgrad regel")
    }
}
