package no.nav.helse.person.inntekt

import no.nav.helse.dto.deserialisering.InntektsopplysningInnDto
import no.nav.helse.dto.deserialisering.InntektsopplysningInnDto.ArbeidsgiverinntektDto.KildeDto
import no.nav.helse.dto.serialisering.InntektsopplysningUtDto

internal class Arbeidsgiverinntekt internal constructor(internal val kilde: Kilde) : Inntektsopplysning {

    override fun dto() =
        InntektsopplysningUtDto.ArbeidsgiverinntektDto(kilde = kilde.dto())

    internal enum class Kilde {
        Arbeidsgiver,
        AOrdningen;

        fun dto() = when (this) {
            Arbeidsgiver -> InntektsopplysningUtDto.ArbeidsgiverinntektDto.KildeDto.Arbeidsgiver
            AOrdningen -> InntektsopplysningUtDto.ArbeidsgiverinntektDto.KildeDto.AOrdningen
        }

        companion object {
            fun gjenopprett(dto: KildeDto) = when (dto) {
                KildeDto.Arbeidsgiver -> Arbeidsgiver
                KildeDto.AOrdningen -> AOrdningen
            }
        }
    }

    internal companion object {
        internal fun fraInntektsmelding(inntektsmeldinginntekt: Inntektsmeldinginntekt) =
            Arbeidsgiverinntekt(
                kilde = when (inntektsmeldinginntekt.kilde) {
                    Inntektsmeldinginntekt.Kilde.Arbeidsgiver -> Kilde.Arbeidsgiver
                    Inntektsmeldinginntekt.Kilde.AOrdningen -> Kilde.AOrdningen
                }
            )

        internal fun gjenopprett(dto: InntektsopplysningInnDto.ArbeidsgiverinntektDto): Arbeidsgiverinntekt {
            return Arbeidsgiverinntekt(
                kilde = Kilde.gjenopprett(dto.kilde),
            )
        }
    }
}
