package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate

data class Utbetalingslinje(
    val fom: LocalDate,
    var tom: LocalDate,
    val dagsats: Int,
    val grad: Double
)
