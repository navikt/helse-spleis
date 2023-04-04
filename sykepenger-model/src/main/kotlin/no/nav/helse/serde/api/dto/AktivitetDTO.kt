package no.nav.helse.serde.api.dto

import java.util.UUID

data class AktivitetDTO(
    val vedtaksperiodeId: UUID,
    val alvorlighetsgrad: String,
    val melding: String,
    val tidsstempel: String
)