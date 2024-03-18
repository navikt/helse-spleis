package no.nav.helse.dto.deserialisering

import no.nav.helse.dto.InntektbeløpDto

data class SykepengegrunnlagInnDto(
    val arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysningInnDto>,
    val deaktiverteArbeidsforhold: List<ArbeidsgiverInntektsopplysningInnDto>,
    val vurdertInfotrygd: Boolean,
    val sammenligningsgrunnlag: SammenligningsgrunnlagInnDto,
    val `6G`: InntektbeløpDto.Årlig
)