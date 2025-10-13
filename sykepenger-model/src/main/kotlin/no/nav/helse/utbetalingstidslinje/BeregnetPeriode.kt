package no.nav.helse.utbetalingstidslinje

import java.util.UUID

internal data class BeregnetPeriode(
    val vedtaksperiodeId: UUID,
    val maksdatoresultat: Maksdatoresultat,
    val utbetalingstidslinje: Utbetalingstidslinje
)
