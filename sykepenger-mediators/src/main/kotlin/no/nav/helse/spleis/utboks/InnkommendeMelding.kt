package no.nav.helse.spleis.utboks

import java.time.LocalDateTime
import no.nav.helse.Personidentifikator
import no.nav.helse.hendelser.MeldingsreferanseId

internal data class InnkommendeMelding(
    val navn: String,
    val meldingsreferanseId: MeldingsreferanseId,
    val personidentifikator: Personidentifikator,
    val opprettet: LocalDateTime,
    val behov: List<String>? = null
)
