package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.UUID

sealed interface Behandlingsavgjørelse : Hendelse {
    fun relevantVedtaksperiode(id: UUID): Boolean
    fun saksbehandler(): Saksbehandler
    val utbetalingId: UUID
    val godkjent: Boolean
    val avgjørelsestidspunkt: LocalDateTime
    val automatisert: Boolean
}

val Behandlingsavgjørelse.vurdering
    get() = saksbehandler().vurdering(
        godkjent = godkjent,
        avgjørelsestidspunkt = avgjørelsestidspunkt,
        automatisert = automatisert
    )
