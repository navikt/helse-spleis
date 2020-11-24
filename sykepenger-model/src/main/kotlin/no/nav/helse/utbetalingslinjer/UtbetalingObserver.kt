package no.nav.helse.utbetalingslinjer

import no.nav.helse.person.ArbeidstakerHendelse
import java.time.LocalDateTime

internal interface UtbetalingObserver {
    fun utbetalingAnnullert(
        oppdrag: Oppdrag,
        hendelse: ArbeidstakerHendelse,
        godkjentTidspunkt: LocalDateTime,
        saksbehandlerEpost: String
    )
}
