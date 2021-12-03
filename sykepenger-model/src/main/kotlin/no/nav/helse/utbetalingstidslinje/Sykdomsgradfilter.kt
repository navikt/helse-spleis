package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Etterlevelse.Vurderingsresultat.Companion.`§8-13 ledd 1`
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Companion.avvisteDager
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Companion.periode
import no.nav.helse.økonomi.Økonomi
import no.nav.helse.økonomi.Økonomi.Companion.totalSykdomsgrad

internal class Sykdomsgradfilter(
    private val tidslinjer: List<Utbetalingstidslinje>,
    private val periode: Periode,
    private val aktivitetslogg: IAktivitetslogg
) {

    internal fun filter() {
        val dagerUnderGrensen = periode(tidslinjer).filter { dato -> totalSykdomsgrad(tidslinjer.map { it[dato].økonomi }).erUnderGrensen() }
        val avvisteDager = avvisteDager(tidslinjer, dagerUnderGrensen.grupperSammenhengendePerioder(), periode, listOf(Begrunnelse.MinimumSykdomsgrad))
        val harAvvisteDager = avvisteDager.isNotEmpty()
        aktivitetslogg.`§8-13 ledd 1`(oppfylt = !harAvvisteDager, avvisteDager.map { it.dato })
        if (harAvvisteDager)
            return aktivitetslogg.warn("Minst én dag uten utbetaling på grunn av sykdomsgrad under 20 %%. Vurder å sende vedtaksbrev fra Infotrygd")
        aktivitetslogg.info("Ingen avviste dager på grunn av 20 %% samlet sykdomsgrad-regel for denne perioden")
    }
}
