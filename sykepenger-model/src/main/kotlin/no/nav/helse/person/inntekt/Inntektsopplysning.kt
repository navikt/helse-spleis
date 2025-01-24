package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.dto.deserialisering.InntektsdataInnDto
import no.nav.helse.dto.deserialisering.InntektsopplysningInnDto
import no.nav.helse.dto.serialisering.InntektsdataUtDto
import no.nav.helse.dto.serialisering.InntektsopplysningUtDto
import no.nav.helse.økonomi.Inntekt

data class Inntektsdata(
    val hendelseId: UUID,
    val dato: LocalDate,
    val beløp: Inntekt,
    val tidsstempel: LocalDateTime
) {

    fun funksjoneltLik(other: Inntektsdata) =
        this.dato == other.dato && this.beløp == other.beløp

    fun dto() = InntektsdataUtDto(
        hendelseId = hendelseId,
        dato = dato,
        beløp = beløp.dto(),
        tidsstempel = tidsstempel
    )

    companion object {
        fun ingen(hendelseId: UUID, dato: LocalDate, tidsstempel: LocalDateTime = LocalDateTime.now()) = Inntektsdata(
            hendelseId = hendelseId,
            dato = dato,
            beløp = Inntekt.INGEN,
            tidsstempel = tidsstempel
        )

        fun gjenopprett(dto: InntektsdataInnDto) = Inntektsdata(
            hendelseId = dto.hendelseId,
            dato = dto.dato,
            beløp = Inntekt.gjenopprett(dto.beløp),
            tidsstempel = dto.tidsstempel
        )
    }
}

internal sealed interface Inntektsopplysning {

    data object Infotrygd : Inntektsopplysning
    data object Arbeidsgiverinntekt : Inntektsopplysning
    data class SkattSykepengegrunnlag(
        val inntektsopplysninger: List<Skatteopplysning>
    ) : Inntektsopplysning {
        internal companion object {
            internal fun fraSkatt(inntektsopplysningerTreMånederFørSkjæringstidspunkt: List<Skatteopplysning>? = emptyList()) =
                SkattSykepengegrunnlag(inntektsopplysningerTreMånederFørSkjæringstidspunkt ?: emptyList())

            internal fun gjenopprett(dto: InntektsopplysningInnDto.SkattSykepengegrunnlagDto): SkattSykepengegrunnlag {
                val skatteopplysninger = dto.inntektsopplysninger.map { Skatteopplysning.gjenopprett(it) }
                return SkattSykepengegrunnlag(
                    inntektsopplysninger = skatteopplysninger
                )
            }
        }
    }

    companion object {
        internal fun gjenopprett(dto: InntektsopplysningInnDto): Inntektsopplysning {
            return when (dto) {
                is InntektsopplysningInnDto.InfotrygdDto -> Infotrygd
                is InntektsopplysningInnDto.ArbeidsgiverinntektDto -> Arbeidsgiverinntekt
                is InntektsopplysningInnDto.SkattSykepengegrunnlagDto -> SkattSykepengegrunnlag.gjenopprett(dto)
            }
        }
    }

    fun dto() = when (this) {
        Infotrygd -> InntektsopplysningUtDto.InfotrygdDto
        Arbeidsgiverinntekt -> InntektsopplysningUtDto.ArbeidsgiverinntektDto
        is SkattSykepengegrunnlag -> InntektsopplysningUtDto.SkattSykepengegrunnlagDto(
            inntektsopplysninger = inntektsopplysninger.map { it.dto() }
        )
    }
}
