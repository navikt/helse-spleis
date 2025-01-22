package no.nav.helse.dto.serialisering

import java.util.UUID

data class FaktaavklartInntektUtDto(
    val id: UUID,
    val inntektsdata: InntektsdataUtDto,
    val inntektsopplysning: InntektsopplysningUtDto
)
