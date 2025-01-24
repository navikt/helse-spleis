package no.nav.helse.person.inntekt

import no.nav.helse.dto.deserialisering.InntektsopplysningInnDto
import no.nav.helse.dto.serialisering.InntektsopplysningUtDto

internal sealed interface Inntektsopplysning {
    data class Arbeidstaker(val kilde: Arbeidstakerinntektskilde) : Inntektsopplysning {
        override fun dto() = InntektsopplysningUtDto.ArbeidstakerDto(
            kilde = kilde.dto()
        )

        companion object {
            fun gjenopprett(dto: InntektsopplysningInnDto.ArbeidstakerDto) = Arbeidstaker(
                kilde = Arbeidstakerinntektskilde.gjenopprett(dto.kilde)
            )
        }
    }

    fun dto(): InntektsopplysningUtDto

    companion object {
        fun gjenopprett(dto: InntektsopplysningInnDto) = when (dto) {
            is InntektsopplysningInnDto.ArbeidstakerDto -> Arbeidstaker.gjenopprett(dto)
        }
    }
}
