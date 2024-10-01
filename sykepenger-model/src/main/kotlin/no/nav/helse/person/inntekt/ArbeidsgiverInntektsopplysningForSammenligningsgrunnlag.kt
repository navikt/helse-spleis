package no.nav.helse.person.inntekt

import no.nav.helse.dto.ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagDto
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.summer

internal data class ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag(
    val orgnummer: String,
    val inntektsopplysninger: List<Skatteopplysning>
) {
    val rapportertInntekt = Skatteopplysning.rapportertInntekt(inntektsopplysninger)

    internal fun gjelder(organisasjonsnummer: String) = organisasjonsnummer == orgnummer

    internal companion object {

        internal fun List<ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag>.sammenligningsgrunnlag(): Inntekt {
            return map { it.rapportertInntekt }.summer()
        }

        internal fun gjenopprett(dto: ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagDto): ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag {
            return ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag(
                orgnummer = dto.orgnummer,
                inntektsopplysninger = dto.inntektsopplysninger.map { Skatteopplysning.gjenopprett(it) }
            )
        }
    }

    internal fun dto() = ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagDto(
        orgnummer = this.orgnummer,
        inntektsopplysninger = this.inntektsopplysninger.map { it.dto() }
    )
}