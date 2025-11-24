package no.nav.helse.hendelser

import java.time.LocalDateTime
import no.nav.helse.utbetalingslinjer.Utbetaling.Vurdering

class Saksbehandler(val ident: String, val epost: String) {
    override fun toString() = ident
    internal fun vurdering(godkjent: Boolean, avgjørelsestidspunkt: LocalDateTime, automatisert: Boolean) = Vurdering(
        godkjent = godkjent,
        tidspunkt = avgjørelsestidspunkt,
        automatiskBehandling = automatisert,
        ident = ident,
        epost = epost
    )
}
