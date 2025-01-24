package no.nav.helse.dto.serialisering

import no.nav.helse.dto.SkatteopplysningDto

sealed interface InntektsopplysningUtDto {

    data object InfotrygdDto : InntektsopplysningUtDto

    data object ArbeidsgiverDto : InntektsopplysningUtDto

    data class AOrdningenDto(
        val inntektsopplysninger: List<SkatteopplysningDto>
    ) : InntektsopplysningUtDto
}
