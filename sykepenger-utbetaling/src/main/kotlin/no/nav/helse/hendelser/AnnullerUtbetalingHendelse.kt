package no.nav.helse.hendelser

import java.util.UUID
import no.nav.helse.utbetalingslinjer.Utbetaling

interface AnnullerUtbetalingHendelse {
    val utbetalingId: UUID
    val vurdering: Utbetaling.Vurdering
}