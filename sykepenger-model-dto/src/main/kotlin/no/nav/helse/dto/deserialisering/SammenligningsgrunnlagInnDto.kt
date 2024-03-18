package no.nav.helse.dto.deserialisering

import no.nav.helse.dto.ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagDto
import no.nav.helse.dto.InntektbeløpDto

data class SammenligningsgrunnlagInnDto(
    val sammenligningsgrunnlag: InntektbeløpDto.Årlig,
    val arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagDto>,
)