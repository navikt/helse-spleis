package no.nav.helse.person.inntekt

import java.time.Year
import no.nav.helse.dto.deserialisering.InntektsopplysningInnDto
import no.nav.helse.dto.serialisering.InntektsopplysningUtDto
import no.nav.helse.dto.serialisering.InntektsopplysningUtDto.SelvstendigDto.PensjonsgivendeInntektDto
import no.nav.helse.økonomi.Inntekt

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

    data class Selvstendig(val pensjonsgivendeInntekt: List<PensjonsgivendeInntekt>, val anvendtGrunnbeløp: Inntekt) : Inntektsopplysning {
        override fun dto() = InntektsopplysningUtDto.SelvstendigDto(
            pensjonsgivendeInntekt = pensjonsgivendeInntekt.map {
                PensjonsgivendeInntektDto(it.årstall, it.beløp.dto())
            },
            anvendtGrunnbeløp = anvendtGrunnbeløp.dto()
        )

        data class PensjonsgivendeInntekt(val årstall: Year, val beløp: Inntekt)
        companion object {
            fun gjenopprett(dto: InntektsopplysningInnDto.SelvstendigDto) = Selvstendig(
                pensjonsgivendeInntekt = dto.pensjonsgivendeInntekt.map {
                    PensjonsgivendeInntekt(it.årstall, Inntekt.gjenopprett(it.beløp))
                },
                anvendtGrunnbeløp = Inntekt.gjenopprett(dto.anvendtGrunnbeløp)
            )
        }
    }

    fun dto(): InntektsopplysningUtDto

    companion object {
        fun gjenopprett(dto: InntektsopplysningInnDto) = when (dto) {
            is InntektsopplysningInnDto.ArbeidstakerDto -> Arbeidstaker.gjenopprett(dto)
            is InntektsopplysningInnDto.SelvstendigDto -> Selvstendig.gjenopprett(dto)
        }
    }
}
