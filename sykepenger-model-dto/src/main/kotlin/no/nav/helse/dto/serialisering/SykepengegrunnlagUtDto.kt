package no.nav.helse.dto.serialisering

import no.nav.helse.dto.InntektDto

data class SykepengegrunnlagUtDto(
    val arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysningUtDto>,
    val deaktiverteArbeidsforhold: List<ArbeidsgiverInntektsopplysningUtDto>,
    val vurdertInfotrygd: Boolean,
    val sammenligningsgrunnlag: SammenligningsgrunnlagUtDto,
    val `6G`: InntektDto,
    val sykepengegrunnlag: InntektDto,
    val totalOmregnetÅrsinntekt: InntektDto,
    val beregningsgrunnlag: InntektDto,
    val er6GBegrenset: Boolean,
    val forhøyetInntektskrav: Boolean,
    val minsteinntekt: InntektDto,
    val oppfyllerMinsteinntektskrav: Boolean
)