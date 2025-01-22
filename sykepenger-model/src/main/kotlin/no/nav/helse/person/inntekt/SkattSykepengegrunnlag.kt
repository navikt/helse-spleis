package no.nav.helse.person.inntekt

import no.nav.helse.dto.deserialisering.InntektsopplysningInnDto
import no.nav.helse.dto.serialisering.InntektsopplysningUtDto

internal class SkattSykepengegrunnlag(
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

    override fun dto() =
        InntektsopplysningUtDto.SkattSykepengegrunnlagDto(
            inntektsopplysninger = inntektsopplysninger.map { it.dto() }
        )
}
