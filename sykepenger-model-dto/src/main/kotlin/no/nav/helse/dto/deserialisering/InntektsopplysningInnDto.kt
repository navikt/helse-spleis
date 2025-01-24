package no.nav.helse.dto.deserialisering

import no.nav.helse.dto.SkatteopplysningDto

sealed interface InntektsopplysningInnDto {
    data object InfotrygdDto : InntektsopplysningInnDto
    data object ArbeidsgiverinntektDto : InntektsopplysningInnDto

    data class SkattSykepengegrunnlagDto(
        val inntektsopplysninger: List<SkatteopplysningDto>
    ) : InntektsopplysningInnDto
}
