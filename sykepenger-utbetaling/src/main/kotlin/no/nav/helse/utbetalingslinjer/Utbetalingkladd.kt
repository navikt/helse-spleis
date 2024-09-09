package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.Periode
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje

class Utbetalingkladd(
    val utbetalingsperiode: Periode,
    val arbeidsgiveroppdrag: Oppdrag,
    val personoppdrag: Oppdrag,
    val utbetalingstidslinje: Utbetalingstidslinje
)