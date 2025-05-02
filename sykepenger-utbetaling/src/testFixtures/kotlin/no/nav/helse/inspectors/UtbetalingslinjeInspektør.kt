package no.nav.helse.inspectors

import no.nav.helse.utbetalingslinjer.Utbetalingslinje

val Utbetalingslinje.inspektør get() = UtbetalingslinjeInspektør(this)

class UtbetalingslinjeInspektør(utbetalingslinje: Utbetalingslinje) {
    val endringskode = utbetalingslinje.endringskode
    val fom = utbetalingslinje.fom
    val tom = utbetalingslinje.tom
    val periode = utbetalingslinje.periode
    val beløp = utbetalingslinje.beløp
    val grad = utbetalingslinje.grad
    val klassekode = utbetalingslinje.klassekode
    val delytelseId = utbetalingslinje.delytelseId
    val refDelytelseId = utbetalingslinje.refDelytelseId
    val refFagsystemId = utbetalingslinje.refFagsystemId
    val datoStatusFom = utbetalingslinje.datoStatusFom
    val statuskode = utbetalingslinje.statuskode
}
