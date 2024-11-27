package no.nav.helse.hendelser

import no.nav.helse.utbetalingslinjer.Oppdragstatus
import java.time.LocalDateTime
import java.util.UUID

interface UtbetalingmodulHendelse {
    val utbetalingId: UUID
    val fagsystemId: String
    val avstemmingsnøkkel: Long
    val overføringstidspunkt: LocalDateTime
    val melding: String
    val status: Oppdragstatus
}
