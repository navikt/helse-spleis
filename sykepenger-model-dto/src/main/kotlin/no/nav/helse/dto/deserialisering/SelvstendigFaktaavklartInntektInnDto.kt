package no.nav.helse.dto.deserialisering

import java.time.Year
import java.util.UUID
import no.nav.helse.dto.InntektbeløpDto

data class SelvstendigFaktaavklartInntektInnDto(
    val id: UUID,
    val inntektsdata: InntektsdataInnDto,
    val pensjonsgivendeInntekter: List<PensjonsgivendeInntektDto>,
    val anvendtGrunnbeløp: InntektbeløpDto.Årlig
) {
    data class PensjonsgivendeInntektDto(val årstall: Year, val beløp: InntektbeløpDto.Årlig)
}

