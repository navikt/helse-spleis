package no.nav.helse.dto.deserialisering

import java.util.UUID

data class SaksbehandlerInnDto(
    val id: UUID,
    val inntektsdata: InntektsdataInnDto
)
