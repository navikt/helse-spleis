package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.util.*
import no.nav.helse.dto.deserialisering.InntektsopplysningInnDto
import no.nav.helse.dto.deserialisering.InntektsopplysningInnDto.ArbeidsgiverinntektDto.KildeDto
import no.nav.helse.dto.serialisering.InntektsmeldingDto
import no.nav.helse.person.PersonObserver.UtkastTilVedtakEvent.Inntektskilde
import no.nav.helse.yearMonth

internal data class Inntektsmeldinginntekt(
    val id: UUID,
    val inntektsdata: Inntektsdata,
    val kilde: Kilde
) {
    internal fun inntektskilde(): Inntektskilde = when (kilde) {
        Kilde.Arbeidsgiver -> Inntektskilde.Arbeidsgiver
        Kilde.AOrdningen -> Inntektskilde.AOrdningen
    }

    internal fun view() = InntektsmeldinginntektView(
        id = id,
        inntektsdata = inntektsdata
    )

    internal fun avklarSykepengegrunnlag(skatt: SkattSykepengegrunnlag): Inntektsopplysning {
        if (skatt.inntektsdata.dato.yearMonth < this.inntektsdata.dato.yearMonth) return skatt
        return Arbeidsgiverinntekt(
            id = UUID.randomUUID(),
            inntektsdata = this.inntektsdata,
            kilde = when (this.kilde) {
                Kilde.Arbeidsgiver -> Arbeidsgiverinntekt.Kilde.Arbeidsgiver
                Kilde.AOrdningen -> Arbeidsgiverinntekt.Kilde.AOrdningen
            }
        )
    }

    internal fun kanLagres(other: Inntektsmeldinginntekt) = this.inntektsdata.hendelseId != other.inntektsdata.hendelseId || this.inntektsdata.dato != other.inntektsdata.dato

    fun dto() =
        InntektsmeldingDto(
            id = id,
            inntektsdata = inntektsdata.dto(),
            kilde = kilde.dto()
        )

    internal enum class Kilde {
        Arbeidsgiver,
        AOrdningen;

        fun dto() = when (this) {
            Arbeidsgiver -> InntektsmeldingDto.KildeDto.Arbeidsgiver
            AOrdningen -> InntektsmeldingDto.KildeDto.AOrdningen
        }

        companion object {
            fun gjenopprett(dto: KildeDto) = when (dto) {
                KildeDto.Arbeidsgiver -> Arbeidsgiver
                KildeDto.AOrdningen -> AOrdningen
            }
        }
    }

    internal companion object {
        internal fun gjenopprett(dto: InntektsopplysningInnDto.ArbeidsgiverinntektDto): Inntektsmeldinginntekt {
            return Inntektsmeldinginntekt(
                id = dto.id,
                inntektsdata = Inntektsdata.gjenopprett(dto.inntektsdata),
                kilde = Kilde.gjenopprett(dto.kilde),
            )
        }

        internal fun List<Inntektsmeldinginntekt>.finnInntektsmeldingForSkjæringstidspunkt(
            skjæringstidspunkt: LocalDate,
            førsteFraværsdag: LocalDate?
        ): Inntektsmeldinginntekt? {
            val inntektsmeldinger = this.filter { it.inntektsdata.dato == skjæringstidspunkt || it.inntektsdata.dato == førsteFraværsdag }
            return inntektsmeldinger.maxByOrNull { inntektsmelding -> inntektsmelding.inntektsdata.tidsstempel }
        }
    }
}

internal data class InntektsmeldinginntektView(
    val id: UUID,
    val inntektsdata: Inntektsdata
)
