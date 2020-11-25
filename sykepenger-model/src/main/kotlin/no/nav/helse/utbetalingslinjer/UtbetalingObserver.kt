package no.nav.helse.utbetalingslinjer

import no.nav.helse.person.ArbeidstakerHendelse
import java.time.LocalDateTime
import java.util.*

internal interface UtbetalingObserver {
    fun utbetalingAnnullert(
        id: UUID,
        oppdrag: Oppdrag,
        hendelse: ArbeidstakerHendelse,
        godkjenttidspunkt: LocalDateTime,
        saksbehandlerEpost: String
    )

    fun utbetalingUtbetalt(id: UUID, type: Utbetaling.Utbetalingtype, oppdrag: Oppdrag, ident: String, epost: String, tidspunkt: LocalDateTime, automatiskBehandling: Boolean) {}
    fun utbetalingEndret(id: UUID, type: Utbetaling.Utbetalingtype, arbeidsgiverOppdrag: Oppdrag, forrigeTilstand: Utbetaling.Tilstand, nesteTilstand: Utbetaling.Tilstand) {}
}
