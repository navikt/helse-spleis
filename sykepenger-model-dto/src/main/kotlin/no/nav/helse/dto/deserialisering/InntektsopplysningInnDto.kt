package no.nav.helse.dto.deserialisering

import no.nav.helse.dto.SkatteopplysningDto

sealed interface InntektsopplysningInnDto {
    data object InfotrygdDto : InntektsopplysningInnDto
    data object ArbeidsgiverDto : InntektsopplysningInnDto

    data class AOrdningenDto(
        val inntektsopplysninger: List<SkatteopplysningDto>
    ) : InntektsopplysningInnDto
}
