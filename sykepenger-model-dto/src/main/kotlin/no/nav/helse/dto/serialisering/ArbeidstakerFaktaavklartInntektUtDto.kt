package no.nav.helse.dto.serialisering

import java.util.UUID

data class ArbeidstakerFaktaavklartInntektUtDto(
    val id: UUID,
    val inntektsdata: InntektsdataUtDto,
    val inntektsopplysningskilde: ArbeidstakerinntektskildeUtDto
)
