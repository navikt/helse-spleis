package no.nav.helse.dto.serialisering

import java.util.UUID

data class InntektsmeldingDto(
    val id: UUID,
    val inntektsdata: InntektsdataUtDto,
    val kilde: KildeDto
) {
    enum class KildeDto {
        Arbeidsgiver,
        AOrdningen
    }
}
