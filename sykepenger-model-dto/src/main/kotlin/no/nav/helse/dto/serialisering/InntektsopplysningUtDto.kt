package no.nav.helse.dto.serialisering

import no.nav.helse.dto.SkatteopplysningDto

sealed interface InntektsopplysningUtDto {

    data object InfotrygdDto : InntektsopplysningUtDto

    data object ArbeidsgiverinntektDto : InntektsopplysningUtDto

    data class SkattSykepengegrunnlagDto(
        val inntektsopplysninger: List<SkatteopplysningDto>
    ) : InntektsopplysningUtDto
}
