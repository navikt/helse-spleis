package no.nav.helse.dto.deserialisering

import java.util.UUID

data class ArbeidstakerFaktaavklartInntektInnDto(
    override val id: UUID,
    override val inntektsdata: InntektsdataInnDto,
    val inntektsopplysningskilde: ArbeidstakerinntektskildeInnDto
) : FaktaavklartInntektInnDto
