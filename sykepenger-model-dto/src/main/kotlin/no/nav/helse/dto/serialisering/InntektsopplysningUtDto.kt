package no.nav.helse.dto.serialisering

import java.util.*
import no.nav.helse.dto.SkatteopplysningDto

sealed class InntektsopplysningUtDto {
    abstract val id: UUID
    abstract val inntektsdata: InntektsdataUtDto

    data class InfotrygdDto(
        override val id: UUID,
        override val inntektsdata: InntektsdataUtDto
    ) : InntektsopplysningUtDto()

    data class SaksbehandlerDto(
        override val id: UUID,
        override val inntektsdata: InntektsdataUtDto,
        val overstyrtInntekt: UUID,
    ) : InntektsopplysningUtDto()

    data class SkjønnsmessigFastsattDto(
        override val id: UUID,
        override val inntektsdata: InntektsdataUtDto,
        val overstyrtInntekt: UUID,
    ) : InntektsopplysningUtDto()

    data class ArbeidsgiverinntektDto(
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
        val inntektsopplysninger: List<SkatteopplysningDto>
    ) : InntektsopplysningUtDto()
}

