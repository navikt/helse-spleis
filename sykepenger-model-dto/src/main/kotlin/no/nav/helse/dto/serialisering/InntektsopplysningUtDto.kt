package no.nav.helse.dto.serialisering

import java.time.Year
import no.nav.helse.dto.InntektDto

sealed interface InntektsopplysningUtDto {
    data class ArbeidstakerDto(val kilde: ArbeidstakerinntektskildeUtDto) : InntektsopplysningUtDto
    data class SelvstendigDto(val pensjonsgivendeInntekt: List<PensjonsgivendeInntektDto>) : InntektsopplysningUtDto {
        data class PensjonsgivendeInntektDto(val årstall: Year, val beløp: InntektDto)
    }
}
