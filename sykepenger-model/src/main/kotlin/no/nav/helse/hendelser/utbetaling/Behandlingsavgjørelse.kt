package no.nav.helse.hendelser.utbetaling

import java.util.UUID
import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.UtbetalingsavgjørelseHendelse

interface Behandlingsavgjørelse : UtbetalingsavgjørelseHendelse, Hendelse {
    fun relevantVedtaksperiode(id: UUID): Boolean
}