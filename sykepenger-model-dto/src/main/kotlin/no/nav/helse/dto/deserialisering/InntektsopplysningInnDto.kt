package no.nav.helse.dto.deserialisering

import java.util.*
import no.nav.helse.dto.SkatteopplysningDto

sealed class InntektsopplysningInnDto {
    abstract val id: UUID
    abstract val inntektsdata: InntektsdataInnDto

    data class InfotrygdDto(
        override val id: UUID,
        override val inntektsdata: InntektsdataInnDto
    ) : InntektsopplysningInnDto()

    data class SaksbehandlerDto(
        override val id: UUID,
        override val inntektsdata: InntektsdataInnDto,
        val overstyrtInntekt: UUID,
    ) : InntektsopplysningInnDto()

    data class ArbeidsgiverinntektDto(
        override val id: UUID,
        override val inntektsdata: InntektsdataInnDto,
        val kilde: KildeDto
    ) : InntektsopplysningInnDto() {
        enum class KildeDto {
            Arbeidsgiver,
            AOrdningen
        }
    }

    data class SkattSykepengegrunnlagDto(
        override val id: UUID,
        override val inntektsdata: InntektsdataInnDto,
        val inntektsopplysninger: List<SkatteopplysningDto>
    ) : InntektsopplysningInnDto()
}
