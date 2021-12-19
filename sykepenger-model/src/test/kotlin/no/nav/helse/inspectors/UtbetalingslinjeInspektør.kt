package no.nav.helse.inspectors

import no.nav.helse.person.OppdragVisitor
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Klassekode
import no.nav.helse.utbetalingslinjer.Satstype
import no.nav.helse.utbetalingslinjer.Utbetalingslinje
import java.time.LocalDate

internal val Utbetalingslinje.inspektør get() = UtbetalingslinjeInspektør(this)

internal class UtbetalingslinjeInspektør(utbetalingslinje: Utbetalingslinje) : OppdragVisitor {
    internal lateinit var endringskode: Endringskode
        private set
    internal lateinit var fom: LocalDate
        private set

    internal lateinit var tom: LocalDate
        private set

    init {
        utbetalingslinje.accept(this)
    }

    override fun visitUtbetalingslinje(
        linje: Utbetalingslinje,
        fom: LocalDate,
        tom: LocalDate,
        stønadsdager: Int,
        totalbeløp: Int,
        satstype: Satstype,
        beløp: Int?,
        aktuellDagsinntekt: Int?,
        grad: Int?,
        delytelseId: Int,
        refDelytelseId: Int?,
        refFagsystemId: String?,
        endringskode: Endringskode,
        datoStatusFom: LocalDate?,
        statuskode: String?,
        klassekode: Klassekode
    ) {
        this.fom = fom
        this.tom = tom
        this.endringskode = endringskode
    }
}
