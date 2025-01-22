package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.util.*
import no.nav.helse.dto.deserialisering.InntektsmeldingInnDto
import no.nav.helse.dto.serialisering.InntektsmeldingUtDto
import no.nav.helse.person.PersonObserver.UtkastTilVedtakEvent.Inntektskilde

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

    internal fun kanLagres(other: Inntektsmeldinginntekt) = this.inntektsdata.hendelseId != other.inntektsdata.hendelseId || this.inntektsdata.dato != other.inntektsdata.dato

    fun dto() =
        InntektsmeldingUtDto(
            id = id,
            inntektsdata = inntektsdata.dto(),
            kilde = kilde.dto()
        )

    internal enum class Kilde {
        Arbeidsgiver,
        AOrdningen;

        fun dto() = when (this) {
            Arbeidsgiver -> InntektsmeldingUtDto.KildeDto.Arbeidsgiver
            AOrdningen -> InntektsmeldingUtDto.KildeDto.AOrdningen
        }

        companion object {
            fun gjenopprett(dto: InntektsmeldingInnDto.KildeDto) = when (dto) {
                InntektsmeldingInnDto.KildeDto.Arbeidsgiver -> Arbeidsgiver
                InntektsmeldingInnDto.KildeDto.AOrdningen -> AOrdningen
            }
        }
    }

    internal companion object {
        internal fun gjenopprett(dto: InntektsmeldingInnDto): Inntektsmeldinginntekt {
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
