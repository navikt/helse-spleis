package no.nav.helse.sykdomstidslinje

import java.math.BigDecimal
import java.time.LocalDate

data class Utbetalingslinje(
    val fom: LocalDate,
    val tom: LocalDate,
    val dagsats: BigDecimal
)