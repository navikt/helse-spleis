package no.nav.helse.utbetalingstidslinje

import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.utbetalingstidslinje.beregning.Yrkesaktivitet

internal data class BeregnetPeriode(
    val maksdatovurdering: Maksdatovurdering,
    val grunnlagsdata: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement,
    val utbetalingstidslinje: Utbetalingstidslinje,
    val inntektsjusteringer: Map<Yrkesaktivitet, Beløpstidslinje>
)
