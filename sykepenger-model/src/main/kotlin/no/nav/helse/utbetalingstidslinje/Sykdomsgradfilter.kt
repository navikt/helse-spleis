package no.nav.helse.utbetalingstidslinje

import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.etterlevelse.UtbetalingstidslinjeBuilder.Companion.subsumsjonsformat
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_4
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Companion.avvis
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Companion.avvisteDager
import no.nav.helse.økonomi.Prosentdel

internal object Sykdomsgradfilter: UtbetalingstidslinjerFilter {

    override fun filter(
        tidslinjer: List<Utbetalingstidslinje>,
        periode: Periode,
        aktivitetslogg: IAktivitetslogg,
        subsumsjonslogg: Subsumsjonslogg
    ): List<Utbetalingstidslinje> {
        val tidslinjerForSubsumsjon = tidslinjer.subsumsjonsformat()

        val oppdaterte = Utbetalingsdag.totalSykdomsgrad(tidslinjer)

        val dagerUnderGrensen = Utbetalingsdag.dagerUnderGrensen(oppdaterte)

        val avvisteTidslinjer = avvis(oppdaterte, dagerUnderGrensen, listOf(Begrunnelse.MinimumSykdomsgrad))

        Prosentdel.subsumsjon(subsumsjonslogg) { grense ->
            `§ 8-13 ledd 2`(periode, tidslinjerForSubsumsjon, grense, dagerUnderGrensen)
        }
        val avvisteDager = avvisteDager(avvisteTidslinjer, periode, Begrunnelse.MinimumSykdomsgrad)
        val harAvvisteDager = avvisteDager.isNotEmpty()
        subsumsjonslogg.`§ 8-13 ledd 1`(periode, avvisteDager.map { it.dato }.toSortedSet(), tidslinjerForSubsumsjon)
        if (harAvvisteDager) aktivitetslogg.varsel(RV_VV_4)
        else aktivitetslogg.info("Ingen avviste dager på grunn av 20 %% samlet sykdomsgrad-regel for denne perioden")
        return avvisteTidslinjer
    }
}
