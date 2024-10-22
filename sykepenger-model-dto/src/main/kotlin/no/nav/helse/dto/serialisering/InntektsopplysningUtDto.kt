package no.nav.helse.dto.serialisering

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.AnsattPeriodeDto
import no.nav.helse.dto.InntektDto
import no.nav.helse.dto.SkatteopplysningDto
import no.nav.helse.dto.SubsumsjonDto

sealed class InntektsopplysningUtDto {
    abstract val id: UUID
    abstract val hendelseId: UUID
    abstract val dato: LocalDate
    abstract val beløp: InntektDto?
    abstract val tidsstempel: LocalDateTime

    data class IkkeRapportertDto(
        override val id: UUID,
        override val hendelseId: UUID,
        override val dato: LocalDate,
        override val tidsstempel: LocalDateTime
    ) : InntektsopplysningUtDto() {
        override val beløp: InntektDto? = null
    }

    data class InfotrygdDto(
        override val id: UUID,
        override val hendelseId: UUID,
        override val dato: LocalDate,
        override val beløp: InntektDto,
        override val tidsstempel: LocalDateTime
    ) : InntektsopplysningUtDto()

    data class SaksbehandlerDto(
        override val id: UUID,
        override val hendelseId: UUID,
        override val dato: LocalDate,
        override val beløp: InntektDto,
        override val tidsstempel: LocalDateTime,
        val forklaring: String?,
        val subsumsjon: SubsumsjonDto?,
        val overstyrtInntekt: UUID,
    ) : InntektsopplysningUtDto()

    data class SkjønnsmessigFastsattDto(
        override val id: UUID,
        override val hendelseId: UUID,
        override val dato: LocalDate,
        override val beløp: InntektDto,
        override val tidsstempel: LocalDateTime,
        val overstyrtInntekt: UUID,
    ) : InntektsopplysningUtDto()

    data class InntektsmeldingDto(
        override val id: UUID,
        override val hendelseId: UUID,
        override val dato: LocalDate,
        override val beløp: InntektDto,
        override val tidsstempel: LocalDateTime,
        val kilde: KildeDto
    ) : InntektsopplysningUtDto() {
        enum class KildeDto {
            Arbeidsgiver,
            AOrdningen
        }

    }

    data class SkattSykepengegrunnlagDto(
        override val id: UUID,
        override val hendelseId: UUID,
        override val dato: LocalDate,
        override val beløp: InntektDto,
        override val tidsstempel: LocalDateTime,
        val inntektsopplysninger: List<SkatteopplysningDto>,
        val ansattPerioder: List<AnsattPeriodeDto>
    ) : InntektsopplysningUtDto()
}