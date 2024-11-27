package no.nav.helse.hendelser

import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import java.util.UUID

interface UtbetalingpåminnelseHendelse {
    val utbetalingId: UUID
    val status: Utbetalingstatus
}
