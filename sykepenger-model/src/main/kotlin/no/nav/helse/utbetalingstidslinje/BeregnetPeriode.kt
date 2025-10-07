package no.nav.helse.utbetalingstidslinje

import java.util.UUID
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.inntekt.Inntektskilde

internal data class BeregnetPeriode(
    val vedtaksperiodeId: UUID,
    val maksdatovurdering: Maksdatovurdering,
    val grunnlagsdata: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement,
    val utbetalingstidslinje: Utbetalingstidslinje,
    val inntekterForBeregning: Map<Inntektskilde, Beløpstidslinje>
)
