package no.nav.helse.inspectors

import java.time.LocalDate
import no.nav.helse.utbetalingslinjer.OppdragVisitor
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Klassekode
import no.nav.helse.utbetalingslinjer.Satstype
import no.nav.helse.utbetalingslinjer.Utbetalingslinje
import kotlin.properties.Delegates

internal val Utbetalingslinje.inspektør get() = UtbetalingslinjeInspektør(this)

internal class UtbetalingslinjeInspektør(utbetalingslinje: Utbetalingslinje) : OppdragVisitor {
    internal lateinit var endringskode: Endringskode
        private set
    internal lateinit var fom: LocalDate
        private set

    internal lateinit var tom: LocalDate
        private set

    internal var beløp: Int? = null
        private set

    internal var grad: Int? = null
        private set

    internal var delytelseId by Delegates.notNull<Int>()
        private set

    internal var refDelytelseId : Int? = null
        private set

    internal var refFagsystemId: String? = null
        private set

    internal var datoStatusFom: LocalDate? = null
        private set

    internal var statuskode: String? = null
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
        this.delytelseId = delytelseId
        this.refDelytelseId = refDelytelseId
        this.datoStatusFom = datoStatusFom
        this.refFagsystemId = refFagsystemId
        this.beløp = beløp
        this.grad = grad
        this.statuskode = statuskode
    }
}
