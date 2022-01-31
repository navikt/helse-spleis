package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Companion.avvis
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Companion.avvisteDager
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Companion.periode
import no.nav.helse.økonomi.Økonomi.Companion.totalSykdomsgrad

internal class Sykdomsgradfilter(
    private val tidslinjer: List<Utbetalingstidslinje>,
    private val periode: Periode,
    private val aktivitetslogg: IAktivitetslogg,
    private val subsumsjonObserver: SubsumsjonObserver
) {

    internal fun filter() {
        val dagerUnderGrensen = periode(tidslinjer).filter { dato -> totalSykdomsgrad(tidslinjer.map { it[dato].økonomi }).erUnderGrensen() }
        avvis(tidslinjer, dagerUnderGrensen.grupperSammenhengendePerioder(), listOf(Begrunnelse.MinimumSykdomsgrad))
        val avvisteDager = avvisteDager(tidslinjer, periode, Begrunnelse.MinimumSykdomsgrad)
        val harAvvisteDager = avvisteDager.isNotEmpty()
        subsumsjonObserver.`§ 8-13 ledd 1`(oppfylt = !harAvvisteDager, avvisteDager.map { it.dato })
        if (harAvvisteDager)
            return aktivitetslogg.warn("Minst én dag uten utbetaling på grunn av sykdomsgrad under 20 %%. Vurder å sende vedtaksbrev fra Infotrygd")
        aktivitetslogg.info("Ingen avviste dager på grunn av 20 %% samlet sykdomsgrad-regel for denne perioden")
    }
}
