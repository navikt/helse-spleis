package no.nav.helse.dto.serialisering

import no.nav.helse.dto.ArbeidsgiverInntektsopplysningDto
import no.nav.helse.dto.InntektDto
import no.nav.helse.dto.SammenligningsgrunnlagDto

data class SykepengegrunnlagUtDto(
    val arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysningDto>,
    val deaktiverteArbeidsforhold: List<ArbeidsgiverInntektsopplysningDto>,
    val vurdertInfotrygd: Boolean,
    val sammenligningsgrunnlag: SammenligningsgrunnlagDto,
    val `6G`: InntektDto.Årlig,
    val totalOmregnetÅrsinntekt: InntektDto.Årlig,
    val beregningsgrunnlag: InntektDto.Årlig,
    val er6GBegrenset: Boolean,
    val forhøyetInntektskrav: Boolean,
    val minsteinntekt: InntektDto.Årlig,
    val oppfyllerMinsteinntektskrav: Boolean
)