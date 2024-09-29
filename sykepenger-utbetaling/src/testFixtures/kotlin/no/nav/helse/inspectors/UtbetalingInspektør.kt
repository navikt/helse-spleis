package no.nav.helse.inspectors

import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.UtbetalingVisitor
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.utbetalingslinjer.Utbetalingtype
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje

val Utbetaling.inspektør get() = UtbetalingInspektør(this)

class UtbetalingInspektør(utbetaling: Utbetaling) : UtbetalingVisitor {
    val utbetalingId: UUID = utbetaling.id
    val korrelasjonsId: UUID = utbetaling.korrelasjonsId
    val periode: Periode = utbetaling.periode
    val tilstand: Utbetalingstatus = utbetaling.tilstand.status
    val arbeidsgiverOppdrag: Oppdrag = utbetaling.arbeidsgiverOppdrag
    val personOppdrag: Oppdrag = utbetaling.personOppdrag
    val utbetalingstidslinje: Utbetalingstidslinje = utbetaling.utbetalingstidslinje
    val nettobeløp = arbeidsgiverOppdrag.nettoBeløp() + personOppdrag.nettoBeløp()

    val type: Utbetalingtype = utbetaling.type
    var avstemmingsnøkkel: Long? = null
    val erUbetalt get() = tilstand == Utbetalingstatus.IKKE_UTBETALT
    val erForkastet get() = tilstand == Utbetalingstatus.FORKASTET
    val erEtterutbetaling get() = type == Utbetalingtype.ETTERUTBETALING
    val erAnnullering get() = type == Utbetalingtype.ANNULLERING
    val erUtbetalt get() = tilstand == Utbetalingstatus.ANNULLERT || tilstand == Utbetalingstatus.UTBETALT
    val erAvsluttet get() = erUtbetalt || tilstand == Utbetalingstatus.GODKJENT_UTEN_UTBETALING
}
