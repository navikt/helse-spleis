package no.nav.helse.sykdomstidslinje

import java.math.BigDecimal
import java.time.LocalDate

data class Utbetalingslinje(private val startdato: LocalDate, private val dagsats: BigDecimal) {
    var tom = startdato
    fun fom() = startdato
    fun tom() = tom
    fun dagsats() = dagsats
}