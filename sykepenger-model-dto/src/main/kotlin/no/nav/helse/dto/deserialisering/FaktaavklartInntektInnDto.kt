package no.nav.helse.dto.deserialisering

import java.util.UUID

sealed interface FaktaavklartInntektInnDto {
    val id: UUID
    val inntektsdata: InntektsdataInnDto
}
