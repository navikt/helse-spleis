package no.nav.helse.person.inntekt

import java.util.*
import no.nav.helse.dto.deserialisering.InntektsopplysningInnDto
import no.nav.helse.dto.serialisering.InntektsopplysningUtDto

internal class SkattSykepengegrunnlag(
    id: UUID,
    inntektsdata: Inntektsdata,
    val inntektsopplysninger: List<Skatteopplysning>
) : SkatteopplysningSykepengegrunnlag(id, inntektsdata) {
    internal companion object {
        internal fun gjenopprett(dto: InntektsopplysningInnDto.SkattSykepengegrunnlagDto): SkattSykepengegrunnlag {
            val skatteopplysninger = dto.inntektsopplysninger.map { Skatteopplysning.gjenopprett(it) }
            return SkattSykepengegrunnlag(
                id = dto.id,
                inntektsdata = Inntektsdata.gjenopprett(dto.inntektsdata),
                inntektsopplysninger = skatteopplysninger
            )
        }
    }

    override fun dto() =
        InntektsopplysningUtDto.SkattSykepengegrunnlagDto(
            id = id,
            inntektsdata = inntektsdata.dto(),
            inntektsopplysninger = inntektsopplysninger.map { it.dto() }
        )
}
