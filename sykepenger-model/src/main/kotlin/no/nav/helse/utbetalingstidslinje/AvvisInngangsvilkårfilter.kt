package no.nav.helse.utbetalingstidslinje

import no.nav.helse.person.VilkårsgrunnlagHistorikk

internal interface IAvvisInngangsvilkårfilter {
    fun filter(
        tidslinjer: List<Utbetalingstidslinje>
    )
}

internal class AvvisInngangsvilkårfilter(
    private val vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk,
    private val alder: Alder
): IAvvisInngangsvilkårfilter {
    override fun filter(tidslinjer: List<Utbetalingstidslinje>) {
        vilkårsgrunnlagHistorikk.avvisInngangsvilkår(tidslinjer, alder)
    }
}