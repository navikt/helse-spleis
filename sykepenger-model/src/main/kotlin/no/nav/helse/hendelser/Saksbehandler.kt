package no.nav.helse.hendelser

import java.time.LocalDateTime
import no.nav.helse.utbetalingslinjer.Utbetaling.Vurdering

class Saksbehandler(private val ident: String, private val epost: String) {
    override fun toString() = ident
    internal fun vurdering(godkjent: Boolean, avgjørelsestidspunkt: LocalDateTime, automatisert: Boolean) = Vurdering(
        godkjent = godkjent,
        tidspunkt = avgjørelsestidspunkt,
        automatiskBehandling = automatisert,
        ident = ident,
        epost = epost
    )
}
