package no.nav.helse.dto.deserialisering

import no.nav.helse.dto.SkatteopplysningDto

sealed interface InntektsopplysningInnDto {
    data object InfotrygdDto : InntektsopplysningInnDto

    data class ArbeidsgiverinntektDto(
        val kilde: KildeDto
    ) : InntektsopplysningInnDto {
        enum class KildeDto {
            Arbeidsgiver,
            AOrdningen
        }
    }

    data class SkattSykepengegrunnlagDto(
        val inntektsopplysninger: List<SkatteopplysningDto>
    ) : InntektsopplysningInnDto
}
