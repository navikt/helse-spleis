package no.nav.helse.dto.deserialisering

import no.nav.helse.dto.SkatteopplysningDto

sealed interface ArbeidstakerinntektskildeInnDto {
    data object InfotrygdDto : ArbeidstakerinntektskildeInnDto
    data object ArbeidsgiverDto : ArbeidstakerinntektskildeInnDto

    data class AOrdningenDto(
        val inntektsopplysninger: List<SkatteopplysningDto>
    ) : ArbeidstakerinntektskildeInnDto
}

