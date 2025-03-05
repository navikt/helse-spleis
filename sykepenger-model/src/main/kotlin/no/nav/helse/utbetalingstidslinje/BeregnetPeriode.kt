package no.nav.helse.utbetalingstidslinje

import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.inntekt.InntekterForBeregning

internal data class BeregnetPeriode(
    val maksdatovurdering: Maksdatovurdering,
    val grunnlagsdata: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement,
    val utbetalingstidslinje: Utbetalingstidslinje,
    val inntekterForBeregning: InntekterForBeregning
)
