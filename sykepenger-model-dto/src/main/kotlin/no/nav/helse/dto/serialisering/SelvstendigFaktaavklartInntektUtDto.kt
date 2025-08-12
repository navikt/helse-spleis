package no.nav.helse.dto.serialisering

import java.time.Year
import java.util.UUID
import no.nav.helse.dto.InntektDto

data class SelvstendigFaktaavklartInntektUtDto(
    val id: UUID,
    val inntektsdata: InntektsdataUtDto,
    val pensjonsgivendeInntekter: List<PensjonsgivendeInntektDto>,
    val anvendtGrunnbeløp: InntektDto
) {
    data class PensjonsgivendeInntektDto(val årstall: Year, val beløp: InntektDto)
}

