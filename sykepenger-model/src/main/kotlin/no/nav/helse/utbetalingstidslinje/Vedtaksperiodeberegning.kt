package no.nav.helse.utbetalingstidslinje

import java.util.UUID
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.inntekt.Inntektskilde

data class Vedtaksperiodeberegning(
    val vedtaksperiodeId: UUID,
    val utbetalingstidslinje: Utbetalingstidslinje,
    val inntekterForBeregning: Map<Inntektskilde, Beløpstidslinje>
) {
    val periode = utbetalingstidslinje.periode()
}
