package no.nav.helse.hendelser

import no.nav.helse.utbetalingslinjer.Utbetaling
import java.util.UUID

interface AnnullerUtbetalingHendelse {
    val utbetalingId: UUID
    val vurdering: Utbetaling.Vurdering
}
