package no.nav.helse.utbetalingslinjer

import java.time.LocalDateTime

interface UtbetalingVurderingVisitor {
    fun visitVurdering(
        vurdering: Utbetaling.Vurdering,
        ident: String,
        epost: String,
        tidspunkt: LocalDateTime,
        automatiskBehandling: Boolean,
        godkjent: Boolean
    ) {
    }
}