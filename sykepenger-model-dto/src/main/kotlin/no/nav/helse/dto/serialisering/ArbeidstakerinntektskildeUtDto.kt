package no.nav.helse.dto.serialisering

import no.nav.helse.dto.SkatteopplysningDto

sealed interface ArbeidstakerinntektskildeUtDto {

    data object InfotrygdDto : ArbeidstakerinntektskildeUtDto
    data object ArbeidsgiverDto : ArbeidstakerinntektskildeUtDto
    data class AOrdningenDto(
        val inntektsopplysninger: List<SkatteopplysningDto>
    ) : ArbeidstakerinntektskildeUtDto
}
