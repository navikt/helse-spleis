package no.nav.helse.dto.serialisering

import java.util.UUID

data class InntektsmeldingUtDto(
    val id: UUID,
    val inntektsdata: InntektsdataUtDto,
    val kilde: KildeDto
) {
    enum class KildeDto {
        Arbeidsgiver,
        AOrdningen
    }
}
