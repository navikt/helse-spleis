package no.nav.helse.person.beløp

import java.time.LocalDateTime
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.MeldingsreferanseId

data class Kilde(
    val meldingsreferanseId: MeldingsreferanseId,
    val avsender: Avsender,
    val tidsstempel: LocalDateTime
)
