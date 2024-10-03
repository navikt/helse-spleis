package no.nav.helse.hendelser

import java.util.UUID
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.utbetalingslinjer.Utbetalingstatus

interface UtbetalingpåminnelseHendelse : IAktivitetslogg {
    val utbetalingId: UUID
    val status: Utbetalingstatus
}