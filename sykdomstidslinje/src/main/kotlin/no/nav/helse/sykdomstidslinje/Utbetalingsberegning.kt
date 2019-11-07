package no.nav.helse.sykdomstidslinje

import java.math.BigDecimal
import java.time.LocalDate

data class Utbetalingsberegning(
    val maksdato: LocalDate?,
    val utbetalingslinjer: List<Utbetalingslinje>
)

data class Utbetalingslinje(
    val fom: LocalDate,
    val tom: LocalDate,
    val dagsats: BigDecimal
)
