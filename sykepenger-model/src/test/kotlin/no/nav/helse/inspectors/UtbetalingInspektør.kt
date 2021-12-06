package no.nav.helse.inspectors

import no.nav.helse.person.UtbetalingVisitor
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal val Utbetaling.inspektør get() = UtbetalingInspektør(this)

internal class UtbetalingInspektør(utbetaling: Utbetaling) : UtbetalingVisitor {
    lateinit var utbetalingId: UUID
    lateinit var korrelasjonsId: UUID
    lateinit var tilstand: Utbetaling.Tilstand
    lateinit var arbeidsgiverOppdrag: Oppdrag
    lateinit var personOppdrag: Oppdrag
    lateinit var utbetalingstidslinje: Utbetalingstidslinje
    private lateinit var status: Utbetaling.Tilstand
    private lateinit var type: Utbetaling.Utbetalingtype

    val erUbetalt get() = status == Utbetaling.Ubetalt
    val erForkastet get() = status == Utbetaling.Forkastet
    val erEtterutbetaling get() = type == Utbetaling.Utbetalingtype.ETTERUTBETALING
    val erAnnullering get() = type == Utbetaling.Utbetalingtype.ANNULLERING
    val erUtbetalt get() = status == Utbetaling.Annullert || status == Utbetaling.Utbetalt

    init {
        utbetaling.accept(this)
    }

    override fun preVisitUtbetaling(
        utbetaling: Utbetaling,
        id: UUID,
        korrelasjonsId: UUID,
        type: Utbetaling.Utbetalingtype,
        tilstand: Utbetaling.Tilstand,
        tidsstempel: LocalDateTime,
        oppdatert: LocalDateTime,
        arbeidsgiverNettoBeløp: Int,
        personNettoBeløp: Int,
        maksdato: LocalDate,
        forbrukteSykedager: Int?,
        gjenståendeSykedager: Int?,
        stønadsdager: Int,
        beregningId: UUID
    ) {
        utbetalingId = id
        this.korrelasjonsId = korrelasjonsId
        this.tilstand = tilstand
        this.type = type
        this.status = tilstand
    }

    override fun preVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje) {
        this.utbetalingstidslinje = tidslinje
    }

    override fun preVisitArbeidsgiverOppdrag(oppdrag: Oppdrag) {
        this.arbeidsgiverOppdrag = oppdrag
    }

    override fun preVisitPersonOppdrag(oppdrag: Oppdrag) {
        this.personOppdrag = oppdrag
    }
}
