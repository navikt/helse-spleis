package no.nav.helse.dto.serialisering

import java.util.UUID

sealed interface FaktaavklartInntektUtDto {
    val id: UUID
    val inntektsdata: InntektsdataUtDto
}
