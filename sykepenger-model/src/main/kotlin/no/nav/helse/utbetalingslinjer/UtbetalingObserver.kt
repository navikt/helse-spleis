package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.Hendelseskontekst
import no.nav.helse.hendelser.Periode
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal interface UtbetalingObserver {
    fun utbetalingAnnullert(
        hendelseskontekst: Hendelseskontekst,
        id: UUID,
        korrelasjonsId: UUID,
        periode: Periode,
        personFagsystemId: String?,
        godkjenttidspunkt: LocalDateTime,
        saksbehandlerEpost: String,
        saksbehandlerIdent: String,
        arbeidsgiverFagsystemId: String?
    )

    fun utbetalingUtbetalt(
        hendelseskontekst: Hendelseskontekst,
        id: UUID,
        korrelasjonsId: UUID,
        type: Utbetalingtype,
        periode: Periode,
        maksdato: LocalDate,
        forbrukteSykedager: Int,
        gjenståendeSykedager: Int,
        stønadsdager: Int,
        arbeidsgiverOppdrag: Oppdrag,
        personOppdrag: Oppdrag,
        epost: String,
        tidspunkt: LocalDateTime,
        automatiskBehandling: Boolean,
        utbetalingstidslinje: Utbetalingstidslinje,
        ident: String
    ) {
    }

    fun utbetalingUtenUtbetaling(
        hendelseskontekst: Hendelseskontekst,
        id: UUID,
        korrelasjonsId: UUID,
        type: Utbetalingtype,
        periode: Periode,
        maksdato: LocalDate,
        forbrukteSykedager: Int,
        gjenståendeSykedager: Int,
        stønadsdager: Int,
        personOppdrag: Oppdrag,
        ident: String,
        arbeidsgiverOppdrag: Oppdrag,
        tidspunkt: LocalDateTime,
        automatiskBehandling: Boolean,
        utbetalingstidslinje: Utbetalingstidslinje,
        epost: String
    ) {
    }

    fun utbetalingEndret(
        hendelseskontekst: Hendelseskontekst,
        id: UUID,
        type: Utbetalingtype,
        arbeidsgiverOppdrag: Oppdrag,
        personOppdrag: Oppdrag,
        forrigeTilstand: Utbetaling.Tilstand,
        nesteTilstand: Utbetaling.Tilstand
    ) {
    }
}
