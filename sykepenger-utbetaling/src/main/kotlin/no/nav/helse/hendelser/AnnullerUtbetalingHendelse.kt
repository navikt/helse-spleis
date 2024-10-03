package no.nav.helse.hendelser

import java.util.UUID
import no.nav.helse.dto.SimuleringResultatDto
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.utbetalingslinjer.Fagområde
import no.nav.helse.utbetalingslinjer.Utbetaling

interface AnnullerUtbetalingHendelse : IAktivitetslogg {
    val utbetalingId: UUID
    val vurdering: Utbetaling.Vurdering
}