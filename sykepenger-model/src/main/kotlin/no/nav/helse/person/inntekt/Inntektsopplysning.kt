package no.nav.helse.person.inntekt

import java.time.Year
import no.nav.helse.Grunnbeløp.Companion.`1G`
import no.nav.helse.dto.deserialisering.InntektsopplysningInnDto
import no.nav.helse.dto.serialisering.InntektsopplysningUtDto
import no.nav.helse.dto.serialisering.InntektsopplysningUtDto.SelvstendigDto.PensjonsgivendeInntektDto
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.summer
import no.nav.helse.økonomi.Inntekt.Companion.årlig

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
        val inntektsgrunnlag = beregnInntektsgrunnlag(pensjonsgivendeInntekt, anvendtGrunnbeløp)

        override fun dto() = InntektsopplysningUtDto.SelvstendigDto(
            pensjonsgivendeInntekt = pensjonsgivendeInntekt.map {
                PensjonsgivendeInntektDto(it.årstall, it.beløp.dto())
            },
            anvendtGrunnbeløp = anvendtGrunnbeløp.dto()
        )

        data class PensjonsgivendeInntekt(val årstall: Year, val beløp: Inntekt) {
            val snitt = `1G`.snitt(årstall.value)

            // hvor mange G inntekten utgjør
            val antallG = beløp.årlig / snitt.årlig

            // alle inntekter opp til 6g
            val inntekterOppTil6g = antallG.coerceAtMost(SEKS_G)

            // 1/3 av inntekter mellom 6g og 12g
            val enTredjedelAvInntekterMellom6gOg12g = (antallG.coerceIn(SEKS_G, TOLV_G) - SEKS_G) * EN_TREDJEDEL

            val antallGKompensert = inntekterOppTil6g + enTredjedelAvInntekterMellom6gOg12g

            // snitter antall kompenserte G over tre år
            val Q = antallGKompensert / 3.0

            fun justertÅrsgrunnlag(anvendtGrunnbeløp: Inntekt): Inntekt {
                return anvendtGrunnbeløp * Q
            }

            private companion object {
                private const val SEKS_G = 6.0
                private const val TOLV_G = 12.0
                private const val EN_TREDJEDEL = 1 / 3.0
            }
        }

        companion object {
            fun beregnInntektsgrunnlag(inntekter: List<PensjonsgivendeInntekt>, anvendtGrunnbeløp: Inntekt) =
                inntekter
                    .map { it.justertÅrsgrunnlag(anvendtGrunnbeløp) }
                    .summer()
                    .årlig.toInt()
                    .årlig

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
