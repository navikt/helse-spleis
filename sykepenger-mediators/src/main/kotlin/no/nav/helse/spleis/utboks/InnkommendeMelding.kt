package no.nav.helse.spleis.utboks

import no.nav.helse.Personidentifikator
import no.nav.helse.hendelser.MeldingsreferanseId
import java.time.LocalDateTime

internal data class InnkommendeMelding(
    val navn: String,
    val meldingsreferanseId: MeldingsreferanseId,
    val personidentifikator: Personidentifikator,
    val opprettet: LocalDateTime,
    val behov: List<String>? = null,
)
