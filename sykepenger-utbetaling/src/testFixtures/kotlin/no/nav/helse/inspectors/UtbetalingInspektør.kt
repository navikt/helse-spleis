package no.nav.helse.inspectors

import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.UtbetalingView
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.utbetalingslinjer.Utbetalingtype
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje

val Utbetaling.inspektør get() = UtbetalingInspektør(this.view)
val UtbetalingView.inspektør get() = UtbetalingInspektør(this)

class UtbetalingInspektør(view: UtbetalingView) {

    val utbetalingId: UUID = view.id
    val korrelasjonsId: UUID = view.korrelasjonsId
    val periode: Periode = view.periode
    val tilstand: Utbetalingstatus = view.status
    val arbeidsgiverOppdrag: Oppdrag = view.arbeidsgiverOppdrag
    val personOppdrag: Oppdrag = view.personOppdrag
    val utbetalingstidslinje: Utbetalingstidslinje = view.utbetalingstidslinje
    val nettobeløp = arbeidsgiverOppdrag.nettoBeløp() + personOppdrag.nettoBeløp()

    val type: Utbetalingtype = view.type
    var avstemmingsnøkkel: Long? = null
    val erUbetalt get() = tilstand == Utbetalingstatus.IKKE_UTBETALT
    val erForkastet get() = tilstand == Utbetalingstatus.FORKASTET
    val erEtterutbetaling get() = type == Utbetalingtype.ETTERUTBETALING
    val erAnnullering get() = type == Utbetalingtype.ANNULLERING
    val erUtbetalt get() = tilstand == Utbetalingstatus.ANNULLERT || tilstand == Utbetalingstatus.UTBETALT
    val erAvsluttet get() = erUtbetalt || tilstand == Utbetalingstatus.GODKJENT_UTEN_UTBETALING
}
