package no.nav.helse.inspectors

import java.time.LocalDate
import no.nav.helse.hendelser.til
import no.nav.helse.utbetalingslinjer.OppdragVisitor
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Klassekode
import no.nav.helse.utbetalingslinjer.Satstype
import no.nav.helse.utbetalingslinjer.Utbetalingslinje
import kotlin.properties.Delegates

val Utbetalingslinje.inspektør get() = UtbetalingslinjeInspektør(this)

class UtbetalingslinjeInspektør(utbetalingslinje: Utbetalingslinje) : OppdragVisitor {
    lateinit var endringskode: Endringskode
        private set
    lateinit var fom: LocalDate
        private set

    lateinit var tom: LocalDate
        private set

    val periode get() = fom til tom

    var beløp: Int? = null
        private set

    var grad: Int? = null
        private set

    var delytelseId by Delegates.notNull<Int>()
        private set

    var refDelytelseId : Int? = null
        private set

    var refFagsystemId: String? = null
        private set

    var datoStatusFom: LocalDate? = null
        private set

    var statuskode: String? = null
        private set

    init {
        utbetalingslinje.accept(this)
    }

    override fun visitUtbetalingslinje(
        linje: Utbetalingslinje,
        fom: LocalDate,
        tom: LocalDate,
        satstype: Satstype,
        beløp: Int?,
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
