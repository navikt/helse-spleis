package no.nav.helse.dto.serialisering

import java.util.UUID

data class SaksbehandlerUtDto(
    val id: UUID,
    val inntektsdata: InntektsdataUtDto,
)
