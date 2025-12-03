package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.util.*
import no.nav.helse.dto.deserialisering.InntektsmeldingInnDto
import no.nav.helse.dto.serialisering.InntektsmeldingUtDto

internal data class Inntektsmeldinginntekt private constructor(
    val id: UUID,
    val inntektsdata: Inntektsdata,
    val kilde: Kilde
) {
    internal constructor(id: UUID, inntektsdata: Inntektsdata) : this(id, inntektsdata, Kilde.Arbeidsgiver)

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
        // Denne ble brukt da vi hadde tilstanden AvventeAordningen som lagret skatteopplysninger tilbake i inntektshistorikken, det gjør vi ikke lengre!
        // Kanskje vi bare burde migrert dem bort fra inntektshistorikken slik at den var pure Arbeidsgiver (og sånn sett ikke trengte noen kilde..)
        // .. nå bruker vi jo ikke inntektshistorikken til noe funksjonelt lengre, men sparer litt på den + legger til data der i en periode om det skulle være noe data der
        // som burde vært migrert inn et annet sted..
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
