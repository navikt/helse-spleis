package no.nav.helse.dto.deserialisering

import java.time.Year
import java.util.UUID
import no.nav.helse.dto.InntektbeløpDto

data class SelvstendigFaktaavklartInntektInnDto(
    override val id: UUID,
    override val inntektsdata: InntektsdataInnDto,
    val pensjonsgivendeInntekter: List<PensjonsgivendeInntektDto>,
    val anvendtGrunnbeløp: InntektbeløpDto.Årlig
) : FaktaavklartInntektInnDto {
    data class PensjonsgivendeInntektDto(val årstall: Year, val beløp: InntektbeløpDto.Årlig)
}

