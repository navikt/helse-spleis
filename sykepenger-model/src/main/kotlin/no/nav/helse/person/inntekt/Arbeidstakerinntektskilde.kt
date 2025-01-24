package no.nav.helse.person.inntekt

import no.nav.helse.dto.deserialisering.ArbeidstakerinntektskildeInnDto
import no.nav.helse.dto.serialisering.ArbeidstakerinntektskildeUtDto

internal sealed interface Arbeidstakerinntektskilde {

    data object Infotrygd : Arbeidstakerinntektskilde
    data object Arbeidsgiver : Arbeidstakerinntektskilde
    data class AOrdningen(
        val inntektsopplysninger: List<Skatteopplysning>
    ) : Arbeidstakerinntektskilde {
        internal companion object {
            internal fun fraSkatt(inntektsopplysningerTreMånederFørSkjæringstidspunkt: List<Skatteopplysning>? = emptyList()) =
                AOrdningen(inntektsopplysningerTreMånederFørSkjæringstidspunkt ?: emptyList())

            internal fun gjenopprett(dto: ArbeidstakerinntektskildeInnDto.AOrdningenDto): AOrdningen {
                val skatteopplysninger = dto.inntektsopplysninger.map { Skatteopplysning.gjenopprett(it) }
                return AOrdningen(
                    inntektsopplysninger = skatteopplysninger
                )
            }
        }
    }

    companion object {
        internal fun gjenopprett(dto: ArbeidstakerinntektskildeInnDto): Arbeidstakerinntektskilde {
            return when (dto) {
                is ArbeidstakerinntektskildeInnDto.InfotrygdDto -> Infotrygd
                is ArbeidstakerinntektskildeInnDto.ArbeidsgiverDto -> Arbeidsgiver
                is ArbeidstakerinntektskildeInnDto.AOrdningenDto -> AOrdningen.gjenopprett(dto)
            }
        }
    }

    fun dto() = when (this) {
        Infotrygd -> ArbeidstakerinntektskildeUtDto.InfotrygdDto
        Arbeidsgiver -> ArbeidstakerinntektskildeUtDto.ArbeidsgiverDto
        is AOrdningen -> ArbeidstakerinntektskildeUtDto.AOrdningenDto(
            inntektsopplysninger = inntektsopplysninger.map { it.dto() }
        )
    }
}
