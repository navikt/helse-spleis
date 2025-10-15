package no.nav.helse.person

import no.nav.helse.dto.InntektskildeDto

@JvmInline
value class Inntektskilde(val id: String) {
    fun dto() = InntektskildeDto(id)

    companion object {
        fun gjenopprett(dto: InntektskildeDto) = Inntektskilde(dto.id)
    }
}
