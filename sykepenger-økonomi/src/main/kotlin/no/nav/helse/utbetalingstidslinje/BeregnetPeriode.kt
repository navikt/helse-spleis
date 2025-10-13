package no.nav.helse.utbetalingstidslinje

import java.util.UUID

data class BeregnetPeriode(
    val vedtaksperiodeId: UUID,
    val maksdatoresultat: BeregnetMaksdato,
    val utbetalingstidslinje: Utbetalingstidslinje
)
