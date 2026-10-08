package no.nav.helse.dto.deserialisering

import java.util.UUID

data class SkjønnsmessigFastsattInnDto(
    val id: UUID,
    val inntektsdata: InntektsdataInnDto,
)
