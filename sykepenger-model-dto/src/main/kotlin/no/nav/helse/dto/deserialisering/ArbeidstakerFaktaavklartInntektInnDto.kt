package no.nav.helse.dto.deserialisering

import java.util.UUID

data class ArbeidstakerFaktaavklartInntektInnDto(
    val id: UUID,
    val inntektsdata: InntektsdataInnDto,
    val inntektsopplysning: ArbeidstakerRenameMeInnDto
)

data class ArbeidstakerRenameMeInnDto(val kilde: ArbeidstakerinntektskildeInnDto)
