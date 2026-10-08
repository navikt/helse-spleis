package no.nav.helse.dto.serialisering

import java.util.UUID

data class SkjønnsmessigFastsattUtDto(
    val id: UUID,
    val inntektsdata: InntektsdataUtDto,
)
