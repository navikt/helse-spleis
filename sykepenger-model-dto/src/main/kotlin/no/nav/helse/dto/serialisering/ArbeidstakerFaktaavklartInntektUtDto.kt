package no.nav.helse.dto.serialisering

import java.util.UUID

data class ArbeidstakerFaktaavklartInntektUtDto(
    override val id: UUID,
    override val inntektsdata: InntektsdataUtDto,
    val inntektsopplysningskilde: ArbeidstakerinntektskildeUtDto
) : FaktaavklartInntektUtDto
