package no.nav.helse.inspectors

import no.nav.helse.person.UtbetalingstidslinjeberegningVisitor
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinjeberegning

internal val Utbetalingstidslinjeberegning.inspektør get() = UtbetalingstidslinjeberegningInspektør(this)

internal class UtbetalingstidslinjeberegningInspektør(utbetalingstidslinjeberegning: Utbetalingstidslinjeberegning) : UtbetalingstidslinjeberegningVisitor {
    internal lateinit var utbetalingstidslinje: Utbetalingstidslinje

    init {
        utbetalingstidslinjeberegning.accept(this)
    }

    override fun preVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) {
        this.utbetalingstidslinje = tidslinje
    }
}
