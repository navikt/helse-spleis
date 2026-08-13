package no.nav.helse.opptjening.application

import java.time.LocalDate
import java.util.UUID

data class Opptjeningsvurderingsreferanse(
    val fødselsnummer: String,
    val skjæringstidspunkt: LocalDate,
    val vurderingId: UUID,
)
