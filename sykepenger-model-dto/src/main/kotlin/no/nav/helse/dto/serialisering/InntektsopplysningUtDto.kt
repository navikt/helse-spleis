package no.nav.helse.dto.serialisering

import no.nav.helse.dto.SkatteopplysningDto

sealed interface InntektsopplysningUtDto {

    data object InfotrygdDto : InntektsopplysningUtDto

    data class ArbeidsgiverinntektDto(
        val kilde: KildeDto
    ) : InntektsopplysningUtDto {
        enum class KildeDto {
            Arbeidsgiver,
            AOrdningen
        }
    }

    data class SkattSykepengegrunnlagDto(
        val inntektsopplysninger: List<SkatteopplysningDto>
    ) : InntektsopplysningUtDto
}
