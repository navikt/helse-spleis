package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.Periode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal interface UtbetalingObserver {
    fun utbetalingAnnullert(
        id: UUID,
        periode: Periode,
        fagsystemId: String,
        godkjenttidspunkt: LocalDateTime,
        saksbehandlerEpost: String
    )

    fun utbetalingUtbetalt(
        id: UUID,
        type: Utbetaling.Utbetalingtype,
        periode: Periode,
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

    fun utbetalingUtenUtbetaling(
        id: UUID,
        type: Utbetaling.Utbetalingtype,
        periode: Periode,
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
