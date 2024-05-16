package no.nav.helse.utbetalingstidslinje

import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal class AvvisInngangsvilkårfilter(
    private val vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk
): UtbetalingstidslinjerFilter {

    override fun filter(
        tidslinjer: List<Utbetalingstidslinje>,
        periode: Periode,
        aktivitetslogg: IAktivitetslogg,
        subsumsjonslogg: Subsumsjonslogg
    ): List<Utbetalingstidslinje> {
        return vilkårsgrunnlagHistorikk.avvisInngangsvilkår(tidslinjer, periode, subsumsjonslogg)
    }
}