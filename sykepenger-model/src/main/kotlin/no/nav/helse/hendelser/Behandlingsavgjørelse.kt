package no.nav.helse.hendelser

import java.util.UUID

sealed interface Behandlingsavgjørelse : Hendelse, UtbetalingsavgjørelseHendelse {
    fun relevantVedtaksperiode(id: UUID): Boolean
}