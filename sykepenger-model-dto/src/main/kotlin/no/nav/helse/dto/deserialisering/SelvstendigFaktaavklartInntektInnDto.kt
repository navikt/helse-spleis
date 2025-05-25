package no.nav.helse.dto.deserialisering

import java.time.Year
import java.util.UUID
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.dto.deserialisering.SelvstendigRenameMeInnDto.PensjonsgivendeInntektDto

data class SelvstendigFaktaavklartInntektInnDto(
    val id: UUID,
    val inntektsdata: InntektsdataInnDto,
    val pensjonsgivendeInntekt: List<PensjonsgivendeInntektDto>,
    val anvendtGrunnbeløp: InntektbeløpDto.Årlig
)

data class SelvstendigRenameMeInnDto(val pensjonsgivendeInntekt: List<PensjonsgivendeInntektDto>, val anvendtGrunnbeløp: InntektbeløpDto.Årlig) {
    data class PensjonsgivendeInntektDto(val årstall: Year, val beløp: InntektbeløpDto.Årlig)
}

