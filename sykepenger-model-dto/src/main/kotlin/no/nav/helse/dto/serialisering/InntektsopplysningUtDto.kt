package no.nav.helse.dto.serialisering

import java.util.UUID
import no.nav.helse.dto.AnsattPeriodeDto
import no.nav.helse.dto.SkatteopplysningDto
import no.nav.helse.dto.SubsumsjonDto

sealed class InntektsopplysningUtDto {
    abstract val id: UUID
    abstract val inntektsdata: InntektsdataUtDto

    data class IkkeRapportertDto(
        override val id: UUID,
        override val inntektsdata: InntektsdataUtDto
    ) : InntektsopplysningUtDto()

    data class InfotrygdDto(
        override val id: UUID,
        override val inntektsdata: InntektsdataUtDto
    ) : InntektsopplysningUtDto()

    data class SaksbehandlerDto(
        override val id: UUID,
        override val inntektsdata: InntektsdataUtDto,
        val forklaring: String?,
        val subsumsjon: SubsumsjonDto?,
        val overstyrtInntekt: UUID,
    ) : InntektsopplysningUtDto()

    data class SkjønnsmessigFastsattDto(
        override val id: UUID,
        override val inntektsdata: InntektsdataUtDto,
        val overstyrtInntekt: UUID,
    ) : InntektsopplysningUtDto()

    data class InntektsmeldingDto(
        override val id: UUID,
        override val inntektsdata: InntektsdataUtDto,
        val kilde: KildeDto
    ) : InntektsopplysningUtDto() {
        enum class KildeDto {
            Arbeidsgiver,
            AOrdningen
        }

    }

    data class SkattSykepengegrunnlagDto(
        override val id: UUID,
        override val inntektsdata: InntektsdataUtDto,
        val inntektsopplysninger: List<SkatteopplysningDto>,
        val ansattPerioder: List<AnsattPeriodeDto>
    ) : InntektsopplysningUtDto()
}

