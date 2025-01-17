package no.nav.helse.dto.deserialisering

import java.util.*
import no.nav.helse.dto.AnsattPeriodeDto
import no.nav.helse.dto.SkatteopplysningDto
import no.nav.helse.dto.SubsumsjonDto

sealed class InntektsopplysningInnDto {
    abstract val id: UUID
    abstract val inntektsdata: InntektsdataInnDto

    data class IkkeRapportertDto(
        override val id: UUID,
        override val inntektsdata: InntektsdataInnDto,
    ) : InntektsopplysningInnDto()

    data class InfotrygdDto(
        override val id: UUID,
        override val inntektsdata: InntektsdataInnDto
    ) : InntektsopplysningInnDto()

    data class SaksbehandlerDto(
        override val id: UUID,
        override val inntektsdata: InntektsdataInnDto,
        val forklaring: String?,
        val subsumsjon: SubsumsjonDto?,
        val overstyrtInntekt: UUID,
    ) : InntektsopplysningInnDto()

    data class SkjønnsmessigFastsattDto(
        override val id: UUID,
        override val inntektsdata: InntektsdataInnDto,
        val overstyrtInntekt: UUID,
    ) : InntektsopplysningInnDto()

    data class InntektsmeldingDto(
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
        val inntektsopplysninger: List<SkatteopplysningDto>,
        val ansattPerioder: List<AnsattPeriodeDto>
    ) : InntektsopplysningInnDto()
}
