package no.nav.helse.person.beløp

import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.MeldingsreferanseId
import java.time.LocalDateTime

data class Kilde(
    val meldingsreferanseId: MeldingsreferanseId,
    val avsender: Avsender,
    val tidsstempel: LocalDateTime,
)
