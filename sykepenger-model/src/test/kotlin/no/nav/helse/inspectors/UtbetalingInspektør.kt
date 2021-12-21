package no.nav.helse.inspectors

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.UtbetalingVisitor
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetaling.Utbetalingtype
import no.nav.helse.utbetalingslinjer.Utbetaling.Utbetalt
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal val Utbetaling.inspektør get() = UtbetalingInspektør(this)

internal class UtbetalingInspektør(utbetaling: Utbetaling) : UtbetalingVisitor {
    lateinit var utbetalingId: UUID
        private set
    lateinit var korrelasjonsId: UUID
        private set
    lateinit var periode: Periode
        private set
    lateinit var tilstand: Utbetaling.Tilstand
        private set
    lateinit var arbeidsgiverOppdrag: Oppdrag
        private set
    lateinit var personOppdrag: Oppdrag
        private set
    lateinit var utbetalingstidslinje: Utbetalingstidslinje
        private set
    private lateinit var status: Utbetaling.Tilstand
    private lateinit var type: Utbetalingtype
    var avstemmingsnøkkel: Long? = null
    val erUbetalt get() = status == Utbetaling.Ubetalt
    val erForkastet get() = status == Utbetaling.Forkastet
    val erEtterutbetaling get() = type == Utbetalingtype.ETTERUTBETALING
    val erAnnullering get() = type == Utbetalingtype.ANNULLERING
    val erUtbetalt get() = status == Utbetaling.Annullert || status == Utbetalt

    init {
        utbetaling.accept(this)
    }

    override fun preVisitUtbetaling(
        utbetaling: Utbetaling,
        id: UUID,
        korrelasjonsId: UUID,
        type: Utbetalingtype,
        tilstand: Utbetaling.Tilstand,
        periode: Periode,
        tidsstempel: LocalDateTime,
        oppdatert: LocalDateTime,
        arbeidsgiverNettoBeløp: Int,
        personNettoBeløp: Int,
        maksdato: LocalDate,
        forbrukteSykedager: Int?,
        gjenståendeSykedager: Int?,
        stønadsdager: Int,
        beregningId: UUID,
        overføringstidspunkt: LocalDateTime?,
        avsluttet: LocalDateTime?,
        avstemmingsnøkkel: Long?
    ) {
        utbetalingId = id
        this.periode = periode
        this.korrelasjonsId = korrelasjonsId
        this.tilstand = tilstand
        this.type = type
        this.status = tilstand
        this.avstemmingsnøkkel = avstemmingsnøkkel
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
