package no.nav.helse.dto.deserialisering

import java.util.UUID

data class ArbeidstakerFaktaavklartInntektInnDto(
    val id: UUID,
    val inntektsdata: InntektsdataInnDto,
    val inntektsopplysningskilde: ArbeidstakerinntektskildeInnDto
)
