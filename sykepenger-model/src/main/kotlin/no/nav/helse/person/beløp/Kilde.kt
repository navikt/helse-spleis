package no.nav.helse.person.beløp

import java.util.UUID
import no.nav.helse.hendelser.Avsender

data class Kilde(
    val meldingsreferanseId: UUID,
    val avsender: Avsender
)