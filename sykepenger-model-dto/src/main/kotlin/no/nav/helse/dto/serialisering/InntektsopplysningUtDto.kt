package no.nav.helse.dto.serialisering

sealed interface InntektsopplysningUtDto {
    data class ArbeidstakerDto(val kilde: ArbeidstakerinntektskildeUtDto) : InntektsopplysningUtDto
}
