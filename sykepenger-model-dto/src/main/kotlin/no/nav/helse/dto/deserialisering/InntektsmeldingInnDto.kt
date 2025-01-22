package no.nav.helse.dto.deserialisering

import java.util.*

data class InntektsmeldingInnDto(
    val id: UUID,
    val inntektsdata: InntektsdataInnDto,
    val kilde: KildeDto
) {
    enum class KildeDto {
        Arbeidsgiver,
        AOrdningen
    }
}
