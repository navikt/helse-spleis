package no.nav.helse.utbetalingstidslinje

import java.util.UUID
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.utbetalingstidslinje.beregning.Yrkesaktivitet

data class Vedtaksperiodeberegning(
    val vedtaksperiodeId: UUID,
    val utbetalingstidslinje: Utbetalingstidslinje,
    val inntektsjusteringer: Map<Yrkesaktivitet, Beløpstidslinje> = emptyMap()
) {
    val periode = utbetalingstidslinje.periode()
}
