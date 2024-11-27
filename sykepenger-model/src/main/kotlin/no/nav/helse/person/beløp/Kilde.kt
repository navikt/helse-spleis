package no.nav.helse.person.beløp

import no.nav.helse.hendelser.Avsender
import java.time.LocalDateTime
import java.util.UUID

data class Kilde(
    val meldingsreferanseId: UUID,
    val avsender: Avsender,
    val tidsstempel: LocalDateTime,
)
