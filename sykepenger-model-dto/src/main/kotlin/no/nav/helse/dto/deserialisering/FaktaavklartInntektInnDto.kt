package no.nav.helse.dto.deserialisering

import java.util.UUID

data class FaktaavklartInntektInnDto(
    val id: UUID,
    val inntektsdata: InntektsdataInnDto,
    val inntektsopplysning: InntektsopplysningInnDto
)
