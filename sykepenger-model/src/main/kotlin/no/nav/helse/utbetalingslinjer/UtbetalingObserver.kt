package no.nav.helse.utbetalingslinjer

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal interface UtbetalingObserver {
    fun utbetalingAnnullert(
        id: UUID,
        oppdrag: Oppdrag,
        godkjenttidspunkt: LocalDateTime,
        saksbehandlerEpost: String
    )

    fun utbetalingUtbetalt(
        id: UUID,
        type: Utbetaling.Utbetalingtype,
        maksdato: LocalDate,
        forbrukteSykedager: Int,
        gjenståendeSykedager: Int,
        arbeidsgiverOppdrag: Oppdrag,
        personOppdrag: Oppdrag,
        ident: String,
        epost: String,
        tidspunkt: LocalDateTime,
        automatiskBehandling: Boolean
    ) {
    }

    fun utbetalingEndret(
        id: UUID,
        type: Utbetaling.Utbetalingtype,
        arbeidsgiverOppdrag: Oppdrag,
        personOppdrag: Oppdrag,
        forrigeTilstand: Utbetaling.Tilstand,
        nesteTilstand: Utbetaling.Tilstand
    ) {
    }
}
