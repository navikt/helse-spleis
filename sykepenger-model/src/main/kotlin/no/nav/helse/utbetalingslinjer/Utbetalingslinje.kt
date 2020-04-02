package no.nav.helse.utbetalingslinjer

import java.time.LocalDate

data class Utbetalingslinje(
    var fom: LocalDate,
    var tom: LocalDate,
    var dagsats: Int,
    val grad: Double
)
