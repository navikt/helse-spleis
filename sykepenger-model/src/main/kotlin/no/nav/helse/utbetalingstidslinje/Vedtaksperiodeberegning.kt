package no.nav.helse.utbetalingstidslinje

import java.util.UUID

data class Vedtaksperiodeberegning(
    val vedtaksperiodeId: UUID,
    val utbetalingstidslinje: Utbetalingstidslinje
) {
    val periode = utbetalingstidslinje.periode()
}
