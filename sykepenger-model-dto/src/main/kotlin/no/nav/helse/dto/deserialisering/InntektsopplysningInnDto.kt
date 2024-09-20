package no.nav.helse.dto.deserialisering

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.AnsattPeriodeDto
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.dto.SkatteopplysningDto
import no.nav.helse.dto.SubsumsjonDto

sealed class InntektsopplysningInnDto {
    abstract val id: UUID
    abstract val hendelseId: UUID
    abstract val dato: LocalDate
    abstract val beløp: InntektbeløpDto.MånedligDouble?
    abstract val tidsstempel: LocalDateTime

    data class IkkeRapportertDto(
        override val id: UUID,
        override val hendelseId: UUID,
        override val dato: LocalDate,
        override val tidsstempel: LocalDateTime
    ) : InntektsopplysningInnDto() {
        override val beløp: InntektbeløpDto.MånedligDouble? = null
    }

    data class InfotrygdDto(
        override val id: UUID,
        override val hendelseId: UUID,
        override val dato: LocalDate,
        override val beløp: InntektbeløpDto.MånedligDouble,
        override val tidsstempel: LocalDateTime
    ) : InntektsopplysningInnDto()

    data class InntektFraSøknadDto(
        override val id: UUID,
        override val hendelseId: UUID,
        override val dato: LocalDate,
        override val beløp: InntektbeløpDto.MånedligDouble,
        override val tidsstempel: LocalDateTime
    ) : InntektsopplysningInnDto()

    data class SaksbehandlerDto(
        override val id: UUID,
        override val hendelseId: UUID,
        override val dato: LocalDate,
        override val beløp: InntektbeløpDto.MånedligDouble,
        override val tidsstempel: LocalDateTime,
        val forklaring: String?,
        val subsumsjon: SubsumsjonDto?,
        val overstyrtInntekt: UUID,
    ) : InntektsopplysningInnDto()

    data class SkjønnsmessigFastsattDto(
        override val id: UUID,
        override val hendelseId: UUID,
        override val dato: LocalDate,
        override val beløp: InntektbeløpDto.MånedligDouble,
        override val tidsstempel: LocalDateTime,
        val overstyrtInntekt: UUID,
    ) : InntektsopplysningInnDto()

    data class InntektsmeldingDto(
        override val id: UUID,
        override val hendelseId: UUID,
        override val dato: LocalDate,
        override val beløp: InntektbeløpDto.MånedligDouble,
        override val tidsstempel: LocalDateTime,
        val kilde: KildeDto
    ) : InntektsopplysningInnDto() {
        enum class KildeDto {
            Arbeidsgiver,
            AOrdningen
        }
    }
    data class SkattSykepengegrunnlagDto(
        override val id: UUID,
        override val hendelseId: UUID,
        override val dato: LocalDate,
        override val tidsstempel: LocalDateTime,
        val inntektsopplysninger: List<SkatteopplysningDto>,
        val ansattPerioder: List<AnsattPeriodeDto>
    ) : InntektsopplysningInnDto() {
        override val beløp: InntektbeløpDto.MånedligDouble? = null
    }
}