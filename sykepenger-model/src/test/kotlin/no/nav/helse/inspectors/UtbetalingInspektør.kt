package no.nav.helse.inspectors

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.UtbetalingVisitor
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetaling.Utbetalingtype
import no.nav.helse.utbetalingslinjer.Utbetaling.Utbetalt
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import kotlin.properties.Delegates

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
    var nettobeløp by Delegates.notNull<Int>()
        private set
    private lateinit var status: Utbetaling.Tilstand
    internal lateinit var type: Utbetalingtype
        private set
    internal var forbrukteSykedager by Delegates.notNull<Int>()
        private set
    internal var gjenståendeSykedager by Delegates.notNull<Int>()
        private set
    internal lateinit var maksdato: LocalDate
        private set
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
        this.nettobeløp = arbeidsgiverNettoBeløp + personNettoBeløp
        this.forbrukteSykedager = forbrukteSykedager ?: -1
        this.gjenståendeSykedager = gjenståendeSykedager ?: -1
        this.maksdato = maksdato
    }

    override fun preVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje, gjeldendePeriode: Periode?) {
        this.utbetalingstidslinje = tidslinje
    }

    override fun preVisitArbeidsgiverOppdrag(oppdrag: Oppdrag) {
        this.arbeidsgiverOppdrag = oppdrag
    }

    override fun preVisitPersonOppdrag(oppdrag: Oppdrag) {
        this.personOppdrag = oppdrag
    }
}
