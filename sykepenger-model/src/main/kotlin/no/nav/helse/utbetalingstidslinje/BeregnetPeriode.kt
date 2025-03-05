package no.nav.helse.utbetalingstidslinje

import no.nav.helse.person.VilkårsgrunnlagHistorikk

internal data class BeregnetPeriode(
    val maksdatovurdering: Maksdatovurdering,
    val grunnlagsdata: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement,
    val utbetalingstidslinje: Utbetalingstidslinje
)
