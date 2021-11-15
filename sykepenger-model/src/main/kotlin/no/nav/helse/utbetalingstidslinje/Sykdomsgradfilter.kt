package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Etterlevelse.Vurderingsresultat.Companion.`§8-13 ledd 1`
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
        aktivitetslogg.`§8-13 ledd 1`(oppfylt = avvisteDager.isEmpty())
        if (Utbetalingstidslinje.avvis(tidslinjer, avvisteDager.grupperSammenhengendePerioder(), periode, listOf(Begrunnelse.MinimumSykdomsgrad)))
            return aktivitetslogg.warn("Minst én dag uten utbetaling på grunn av sykdomsgrad under 20 %%. Vurder å sende vedtaksbrev fra Infotrygd")
        aktivitetslogg.info("Ingen avviste dager på grunn av 20 %% samlet sykdomsgrad-regel for denne perioden")
    }

}
