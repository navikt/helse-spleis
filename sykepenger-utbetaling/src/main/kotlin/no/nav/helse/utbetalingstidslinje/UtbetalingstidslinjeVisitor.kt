package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.økonomi.Økonomi

interface UtbetalingstidslinjeVisitor : UtbetalingsdagVisitor {
    /**
     * gjeldendePeriode vil være null om det ikke er noen utbetalingsdager her
     */
    fun preVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje, gjeldendePeriode: Periode?) {}
    fun postVisitUtbetalingstidslinje() {}
}