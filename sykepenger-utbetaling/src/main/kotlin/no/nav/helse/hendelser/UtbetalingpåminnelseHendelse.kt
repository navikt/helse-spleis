package no.nav.helse.hendelser

import java.util.UUID
import no.nav.helse.utbetalingslinjer.Utbetalingstatus

interface UtbetalingpåminnelseHendelse {
    val utbetalingId: UUID
    val status: Utbetalingstatus
}
