package no.nav.helse.dto.deserialisering

sealed interface InntektsopplysningInnDto {
    data class ArbeidstakerDto(val kilde: ArbeidstakerinntektskildeInnDto) : InntektsopplysningInnDto
}
