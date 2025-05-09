package no.nav.helse.dto.deserialisering

import java.time.Year
import no.nav.helse.dto.InntektbeløpDto

sealed interface InntektsopplysningInnDto {
    data class ArbeidstakerDto(val kilde: ArbeidstakerinntektskildeInnDto) : InntektsopplysningInnDto
    data class SelvstendigDto(val pensjonsgivendeInntekt: List<PensjonsgivendeInntektDto>, val anvendtGrunnbeløp: InntektbeløpDto.Årlig) : InntektsopplysningInnDto {
        data class PensjonsgivendeInntektDto(val årstall: Year, val beløp: InntektbeløpDto.Årlig)
    }
}
